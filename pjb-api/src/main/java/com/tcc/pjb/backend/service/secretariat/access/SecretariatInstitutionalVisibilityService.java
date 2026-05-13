package com.tcc.pjb.backend.service.secretariat.access;

import java.util.Collections;
import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.tcc.pjb.backend.core.forum.routing.SecretariatInboxKeyParser;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.EnteFederativo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingResolver;
import com.tcc.pjb.backend.service.secretariat.topology.SecretariatSpecializationResolver;
import com.tcc.pjb.backend.service.secretariat.topology.SecretariatSpecializationResolver.SecretariatSpecializationProfile;

@Service
public class SecretariatInstitutionalVisibilityService {

    private static final Pattern TRIBUNAL_PATTERN = Pattern.compile("\\b(TJ[A-Z]{2}|TRF\\d|TRT\\d+|TRE[-_]?[A-Z]{2}|TSE|STJ|STF|TST|STM|TJM[A-Z]{0,2})\\b");
    private static final Pattern PREFIX_PATTERN = Pattern.compile("^(TRIBUNAL|ORGAO|SECRETARIA|UNIDADE|UJ|VARA|ZONA|GABINETE|INSTANCIA|RAMO)\\s*[:=\\-]\\s*(.+)$");
    private static final Set<String> RECURSAL_QUEUE_MARKERS = Set.of("RECURSAL", "COLEGIADO", "EMBARG", "ADMISS", "REVISAO", "PLENARIO", "ACORDAO", "VOTO");

    private final CurrentUserService currentUserService;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final SecretariatOperationalRoutingResolver routingResolver;
    private final SecretariatInboxAccessService inboxAccessService;
    private final SecretariatSpecializationResolver specializationResolver;

    public SecretariatInstitutionalVisibilityService(CurrentUserService currentUserService,
                                                     ProcessoRepository processoRepository,
                                                     WorkItemRepository workItemRepository,
                                                     SecretariatOperationalRoutingResolver routingResolver,
                                                     SecretariatInboxAccessService inboxAccessService,
                                                     SecretariatSpecializationResolver specializationResolver) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.routingResolver = Objects.requireNonNull(routingResolver);
        this.inboxAccessService = Objects.requireNonNull(inboxAccessService);
        this.specializationResolver = Objects.requireNonNull(specializationResolver);
    }

    public SecretariatOperationalRoutingProfile requireProcessAccess(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        SecretariatOperationalRoutingProfile routing = routingResolver.resolve(processo);
        requireRoutingAccess(routing, processo);
        return routing;
    }

    public WorkItem requireDeskAccess(Long deskWorkItemId) {
        WorkItem desk = workItemRepository.findById(deskWorkItemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("WorkItem", deskWorkItemId));
        Processo processo = desk.getProcesso();
        if (processo != null) {
            requireRoutingAccess(routingResolver.resolve(processo), processo);
        } else {
            ensureInstitutionBorn(resolveActorScope(requireAuthorizedActor()));
        }
        return desk;
    }

    public String requireInboxAccess(String inboxKey) {
        String normalized = inboxAccessService.requireAccess(inboxKey);
        ensureInstitutionBorn(resolveActorScope(requireAuthorizedActor()));
        return normalized;
    }

    public SecretariatInboxInstitutionalProfile describeAuthorizedInbox(String inboxKey) {
        String normalized = requireInboxAccess(inboxKey);
        Usuario actor = requireAuthorizedActor();
        ActorSecretariatScope scope = resolveActorScope(actor);
        SecretariatInboxKeyParser.Parts parts = SecretariatInboxKeyParser.parse(normalized)
                .orElseThrow(() -> forbidden("inbox institucional inválido"));
        String tribunalCodigo = firstNonBlank(scope.tribunalCodigo(), normalizeNullable(parts.org()));
        String instanceClass = firstNonBlank(scope.instanceClass(), resolveInstanceClass(parts.instance()), resolveInstanceClass(parts.lane()));
        String branchClass = firstNonBlank(scope.branchClass(), resolveBranchClass(parts.lane(), tribunalCodigo, actor.getEnteFederativo()), resolveBranchClass(parts.org(), tribunalCodigo, actor.getEnteFederativo()));
        String unitCode = firstNonBlank(scope.unitAnchor(), normalizeNullable(parts.jurisdicao()), normalizeNullable(parts.comarca()));
        String unitDescriptor = firstNonBlank(parts.jurisdicao(), parts.comarca());
        SecretariatSpecializationProfile specialization = specializationResolver.resolve(
                tribunalCodigo,
                firstNonBlank(instanceClass, "PRIMEIRA_INSTANCIA"),
                firstNonBlank(branchClass, "ESTADUAL"),
                firstNonBlank(branchClass, "ESTADUAL"),
                firstNonBlank(scope.specializedSecretariatCode(), unitCode, normalized),
                normalized,
                firstNonBlank(scope.specializedSecretariatCode(), unitCode, normalized) + ":SAN",
                firstNonBlank(scope.specializedSecretariatCode(), unitCode, normalized) + ":AUD",
                firstNonBlank(scope.specializedSecretariatCode(), unitCode, normalized) + ":EXEC",
                "SECRETARIA>" + firstNonBlank(instanceClass, "PRIMEIRA_INSTANCIA") + '>' + firstNonBlank(branchClass, "ESTADUAL"),
                compactMap(
                        "unitCode", unitCode,
                        "unitDescriptor", unitDescriptor,
                        "inboxLane", parts.lane(),
                        "inboxInstance", parts.instance(),
                        "inboxJurisdiction", parts.jurisdicao(),
                        "inboxTerritory", parts.comarca()
                )
        );
        return new SecretariatInboxInstitutionalProfile(normalized, scope, specialization);
    }

    public void requireQueueAccess(String queueCode) {
        Usuario actor = requireAuthorizedActor();
        if (actor.getTipoUsuario() != null && actor.getTipoUsuario().isAdmin()) {
            return;
        }
        ActorSecretariatScope scope = resolveActorScope(actor);
        ensureInstitutionBorn(scope);
        String normalizedQueue = normalizeToken(queueCode);
        if (scope.tribunalCodigo() != null && !normalizedQueue.contains(normalizeToken(scope.tribunalCodigo()))) {
            throw forbidden("fila fora do tribunal institucional da secretaria");
        }
        if (scope.specializedSecretariatCode() != null && !normalizedQueue.contains(normalizeToken(scope.specializedSecretariatCode()))) {
            if (scope.instanceClass() != null && !"PRIMEIRA_INSTANCIA".equals(scope.instanceClass()) && RECURSAL_QUEUE_MARKERS.stream().noneMatch(normalizedQueue::contains)) {
                throw forbidden("fila fora da secretaria recursal institucional do usuario");
            }
        }
        if (scope.branchClass() != null) {
            String marker = queueBranchMarker(scope.branchClass());
            if (marker != null && !normalizedQueue.contains(marker) && !normalizedQueue.contains(normalizeToken(scope.tribunalCodigo()))) {
                throw forbidden("fila fora do ramo institucional da secretaria");
            }
        }
    }

    public void requireRoutingAccess(SecretariatOperationalRoutingProfile routing) {
        requireRoutingAccess(routing, null);
    }

    private void requireRoutingAccess(SecretariatOperationalRoutingProfile routing, Processo processo) {
        Usuario actor = requireAuthorizedActor();
        if (actor.getTipoUsuario() != null && actor.getTipoUsuario().isAdmin()) {
            return;
        }
        ActorSecretariatScope scope = resolveActorScope(actor);
        ensureInstitutionBorn(scope);
        SecretariatSpecializationProfile specialization = routing == null ? null : routing.specialization();
        if (specialization != null) {
            compareAxis(scope.instanceClass(), specialization.secretariatInstanceClass(), "instância da secretaria");
            compareAxis(scope.branchClass(), specialization.secretariatBranchClass(), "ramo da secretaria");
            compareAxis(scope.tribunalCodigo(), routing.tribunalCodigo(), "tribunal da secretaria");
            compareAxis(scope.specializedSecretariatCode(), specialization.specializedSecretariatCode(), "secretaria especializada");
        }
        if (scope.uf() != null && processo != null && processo.getUf() != null && !equalsToken(scope.uf(), processo.getUf())) {
            throw forbidden("processo fora da UF institucional da secretaria");
        }
        if (scope.comarca() != null && processo != null && processo.getComarca() != null
                && scope.instanceClass() != null && "PRIMEIRA_INSTANCIA".equals(scope.instanceClass())
                && !equalsSlug(scope.comarca(), processo.getComarca())) {
            throw forbidden("processo fora da comarca institucional da secretaria");
        }
        String targetUnit = firstNonBlank(
                routing == null || routing.metadata() == null ? null : asString(routing.metadata().get("unidadeJudiciariaCodigo")),
                specialization == null ? null : asString(specialization.metadata().get("unitCode")),
                routing == null ? null : routing.secretariatCode());
        if (scope.unitAnchor() != null && targetUnit != null && !normalizeToken(targetUnit).contains(normalizeToken(scope.unitAnchor()))) {
            throw forbidden("processo fora da unidade institucional da secretaria");
        }
    }

    private Usuario requireAuthorizedActor() {
        Usuario actor = currentUserService.getRequired();
        TipoUsuario tipo = actor.getTipoUsuario();
        if (tipo == null || (!tipo.isServidorJudiciario() && !tipo.isMagistratura() && !tipo.isAdmin())) {
            throw forbidden("perfil não autorizado para operar malha de secretaria");
        }
        return actor;
    }

    private void ensureInstitutionBorn(ActorSecretariatScope scope) {
        if (!scope.institutionBorn()) {
            throw forbidden("secretaria especializada deve nascer de instituição reconhecida e lotação governada");
        }
    }

    private void compareAxis(String actorValue, String targetValue, String label) {
        if (actorValue == null || targetValue == null) {
            return;
        }
        if ("tribunal da secretaria".equals(label) && compatibleTribunal(actorValue, targetValue)) {
            return;
        }
        if ("secretaria especializada".equals(label) && compatibleSecretariat(actorValue, targetValue)) {
            return;
        }
        if (!equalsToken(actorValue, targetValue)) {
            throw forbidden("secretaria fora do recorte institucional de " + label);
        }
    }

    private ActorSecretariatScope resolveActorScope(Usuario actor) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        collectTokens(tokens, actor.getPerfil());
        collectTokens(tokens, actor.getRegistroProfissional());
        for (String item : actor.getEspecialidades()) {
            collectTokens(tokens, item);
        }
        String tribunalCodigo = null;
        String instanceClass = null;
        String branchClass = null;
        String unitAnchor = null;
        String specializedSecretariatCode = null;
        boolean institutionalAnchor = false;
        for (String token : tokens) {
            String normalized = normalizeToken(token);
            if (normalized.isBlank()) {
                continue;
            }
            Matcher tribunalMatcher = TRIBUNAL_PATTERN.matcher(normalized);
            if (tribunalCodigo == null && tribunalMatcher.find()) {
                tribunalCodigo = tribunalMatcher.group(1).replace('_', '-');
                institutionalAnchor = true;
            }
            Matcher prefixMatcher = PREFIX_PATTERN.matcher(token.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
            if (prefixMatcher.matches()) {
                String prefix = normalizeToken(prefixMatcher.group(1));
                String value = normalizeToken(prefixMatcher.group(2));
                switch (prefix) {
                    case "TRIBUNAL", "ORGAO" -> {
                        if (tribunalCodigo == null) {
                            tribunalCodigo = value;
                        }
                        institutionalAnchor = true;
                    }
                    case "SECRETARIA" -> {
                        if (specializedSecretariatCode == null) {
                            specializedSecretariatCode = value;
                        }
                        institutionalAnchor = true;
                    }
                    case "UNIDADE", "UJ", "VARA", "ZONA", "GABINETE" -> {
                        if (unitAnchor == null) {
                            unitAnchor = value;
                        }
                        institutionalAnchor = true;
                    }
                    case "INSTANCIA" -> {
                        if (instanceClass == null) {
                            instanceClass = resolveInstanceClass(value);
                        }
                    }
                    case "RAMO" -> {
                        if (branchClass == null) {
                            branchClass = resolveBranchClass(value, tribunalCodigo, null);
                        }
                    }
                    default -> {
                    }
                }
            }
            if (instanceClass == null) {
                instanceClass = resolveInstanceClass(normalized);
            }
            if (branchClass == null) {
                branchClass = resolveBranchClass(normalized, tribunalCodigo, null);
            }
            if (specializedSecretariatCode == null && normalized.contains("SECRETARIA")) {
                specializedSecretariatCode = normalized;
                institutionalAnchor = true;
            }
        }
        if (branchClass == null) {
            branchClass = resolveBranchClass("", tribunalCodigo, actor.getEnteFederativo());
        }
        if (instanceClass == null && actor.getTipoUsuario() != null && actor.getTipoUsuario().isServidorJudiciario()) {
            instanceClass = "PRIMEIRA_INSTANCIA";
        }
        boolean institutionBorn = institutionalAnchor
                || actor.getTipoUsuario() != null && actor.getTipoUsuario().isServidorJudiciario()
                && (tribunalCodigo != null || actor.getUf() != null || actor.getComarca() != null || actor.getRegistroProfissional() != null || actor.getPerfil() != null);
        return new ActorSecretariatScope(
                tribunalCodigo == null ? inferTribunalByBranch(branchClass, actor.getEnteFederativo()) : tribunalCodigo,
                instanceClass,
                branchClass,
                normalizeNullable(actor.getUf()),
                normalizeNullable(actor.getComarca()),
                unitAnchor,
                specializedSecretariatCode,
                institutionBorn
        );
    }

    private void collectTokens(LinkedHashSet<String> out, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        for (String piece : raw.split("[,;|\\n]")) {
            if (piece != null) {
                String trimmed = piece.trim();
                if (!trimmed.isBlank()) {
                    out.add(trimmed);
                }
            }
        }
    }

    private String resolveInstanceClass(String token) {
        String normalized = normalizeToken(token);
        if (normalized.contains("INSTANCIA_SUPERIOR") || normalized.contains("TRIBUNAL_SUPERIOR") || normalized.contains("SUPERIOR") || normalized.contains("ULTIMA_INSTANCIA")) {
            return "INSTANCIA_SUPERIOR";
        }
        if (normalized.contains("SEGUNDA_INSTANCIA") || normalized.contains("SEGUNDA") || normalized.contains("SEGUNDO_GRAU") || normalized.contains("2G") || normalized.contains("RECURSAL") || normalized.contains("TURMA_RECURSAL")) {
            return "SEGUNDA_INSTANCIA";
        }
        if (normalized.contains("PRIMEIRA_INSTANCIA") || normalized.contains("PRIMEIRA") || normalized.contains("1G") || normalized.contains("VARA") || normalized.contains("COMARCA")) {
            return "PRIMEIRA_INSTANCIA";
        }
        return null;
    }

    private String resolveBranchClass(String token, String tribunalCodigo, EnteFederativo enteFederativo) {
        String normalized = normalizeToken(token);
        if (normalized.contains("JUIZADO") || normalized.contains("TURMA_RECURSAL") || normalized.contains("JE")) {
            return "JUIZADO_ESPECIAL";
        }
        if (normalized.contains("ELEITORAL") || startsWithAny(tribunalCodigo, "TRE", "TSE")) {
            return "ELEITORAL";
        }
        if (normalized.contains("TRABALHISTA") || normalized.contains("TRABALHO") || startsWithAny(tribunalCodigo, "TRT", "TST")) {
            return "TRABALHISTA";
        }
        if (normalized.contains("MILITAR") || startsWithAny(tribunalCodigo, "TJM", "STM")) {
            return "MILITAR";
        }
        if (normalized.contains("FEDERAL") || startsWithAny(tribunalCodigo, "TRF", "STJ", "JF")) {
            return "FEDERAL";
        }
        if (normalized.contains("ESTADUAL") || enteFederativo != null && enteFederativo.isEstadual()) {
            return "ESTADUAL";
        }
        return null;
    }

    private String inferTribunalByBranch(String branchClass, EnteFederativo enteFederativo) {
        if (branchClass == null) {
            return enteFederativo != null && enteFederativo.isEstadual() ? "TJ" : null;
        }
        return switch (branchClass) {
            case "ELEITORAL" -> "TRE";
            case "TRABALHISTA" -> "TRT";
            case "MILITAR" -> "TJM";
            case "FEDERAL" -> "TRF";
            default -> enteFederativo != null && enteFederativo.isEstadual() ? "TJ" : null;
        };
    }

    private boolean compatibleTribunal(String actorValue, String targetValue) {
        String actor = normalizeToken(actorValue);
        String target = normalizeToken(targetValue);
        return actor.equals(target) || target.startsWith(actor) || actor.startsWith(target);
    }

    private boolean compatibleSecretariat(String actorValue, String targetValue) {
        String actor = normalizeToken(actorValue);
        String target = normalizeToken(targetValue);
        return !actor.isBlank() && !target.isBlank() && (actor.equals(target) || target.startsWith(actor + "_") || actor.startsWith(target + "_"));
    }

    private boolean equalsToken(String left, String right) {
        return normalizeToken(left).equals(normalizeToken(right));
    }

    private boolean equalsSlug(String left, String right) {
        return slug(left).equals(slug(right));
    }

    private String queueBranchMarker(String branchClass) {
        return switch (normalizeToken(branchClass)) {
            case "ELEITORAL" -> "TRE";
            case "TRABALHISTA" -> "TRT";
            case "MILITAR" -> "TJM";
            case "FEDERAL" -> "TRF";
            case "JUIZADO_ESPECIAL" -> "JUIZADO";
            case "ESTADUAL" -> "TJ";
            default -> null;
        };
    }

    private boolean startsWithAny(String value, String... prefixes) {
        String normalized = normalizeToken(value);
        for (String prefix : prefixes) {
            if (normalized.startsWith(normalizeToken(prefix))) {
                return true;
            }
        }
        return false;
    }

    private String slug(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private String normalizeToken(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
    }

    private String normalizeNullable(String value) {
        String normalized = value == null ? null : value.trim().toUpperCase(Locale.ROOT);
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private Map<String, Object> compactMap(Object... kv) {
        java.util.LinkedHashMap<String, Object> out = new java.util.LinkedHashMap<>();
        if (kv == null) {
            return Map.of();
        }
        for (int i = 0; i + 1 < kv.length; i += 2) {
            Object key = kv[i];
            Object value = kv[i + 1];
            if (key instanceof String k && value != null && (!(value instanceof CharSequence seq) || !seq.toString().isBlank())) {
                out.put(k, value);
            }
        }
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    public record SecretariatInboxInstitutionalProfile(
            String inboxKey,
            ActorSecretariatScope actorScope,
            SecretariatSpecializationProfile specialization
    ) {

        public SecretariatInboxInstitutionalProfile {
            Objects.requireNonNull(inboxKey);
            Objects.requireNonNull(actorScope);
            Objects.requireNonNull(specialization);
        }

        public Map<String, Object> toMap() {
            java.util.LinkedHashMap<String, Object> out = new java.util.LinkedHashMap<>();
            out.put("inboxKey", inboxKey);
            out.put("actorScope", actorScope.toMap());
            out.put("specialization", specialization.toMap());
            return Collections.unmodifiableMap(out);
        }
    }

    private ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }

    public record ActorSecretariatScope(
            String tribunalCodigo,
            String instanceClass,
            String branchClass,
            String uf,
            String comarca,
            String unitAnchor,
            String specializedSecretariatCode,
            boolean institutionBorn
    ) {

        public Map<String, Object> toMap() {
            java.util.LinkedHashMap<String, Object> out = new java.util.LinkedHashMap<>();
            out.put("tribunalCodigo", tribunalCodigo);
            out.put("instanceClass", instanceClass);
            out.put("branchClass", branchClass);
            out.put("uf", uf);
            out.put("comarca", comarca);
            out.put("unitAnchor", unitAnchor);
            out.put("specializedSecretariatCode", specializedSecretariatCode);
            out.put("institutionBorn", institutionBorn);
            out.values().removeIf(value -> value == null || value instanceof CharSequence seq && seq.toString().isBlank());
            return Collections.unmodifiableMap(out);
        }
    }
}
