package com.tcc.pjb.backend.service.operational.catalog;

import com.tcc.pjb.backend.core.forum.routing.ForumDeskPortfolioProfile;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.secretariat.SecretariatQueueItem;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.service.institutional.support.lane.InstitutionalSupportLaneResolver;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatInstitutionalVisibilityService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class OperationalCompetenceMatrixService {

    public MatrixProjection resolveSecretariat(String inboxKey,
                                              SecretariatInstitutionalVisibilityService.SecretariatInboxInstitutionalProfile profile,
                                              ForumDeskPortfolioProfile portfolio,
                                              Collection<SecretariatQueueItem> items,
                                              Map<Long, Map<String, Object>> metadataByWorkItemId) {
        Objects.requireNonNull(profile);
        Objects.requireNonNull(portfolio);
        OperationalContext context = buildSecretariatContext(inboxKey, profile, portfolio, items, metadataByWorkItemId);
        List<CompetenceRule> rules = new ArrayList<>();
        rules.add(baseRule("PREPARAR_MINUTA_OPERACIONAL", "Preparar minuta operacional", "MINUTA", context, "SERVIDOR_SECRETARIA", false,
                List.of("MINUTA", "ANALISE", "PREPARACAO"), List.of(context.scopeAxis(), context.branchAxis()), List.of("TRIAGEM", "MINUTA")));
        rules.add(baseRule("CONFIRMAR_LOCAL_AUDIENCIA_SESSAO", "Confirmar local de audiência/sessão", "AGENDA", context, "SERVIDOR_SECRETARIA", false,
                List.of("AUDIENCIA", "PAUTA"), List.of(context.scopeAxis(), context.branchAxis()), List.of("AGENDA", "AUDIENCIA")));
        rules.add(baseRule("CONFIRMAR_INTIMACAO_PARTICIPANTES", "Confirmar intimação de participantes", "COMUNICACAO", context, "SERVIDOR_SECRETARIA", false,
                List.of("INTIMACAO", "PARTICIPANTES"), List.of(context.scopeAxis(), context.branchAxis()), List.of("COMUNICACAO")));
        rules.add(baseRule("REGISTRAR_EVENTO_REAL", "Registrar evento real e certidão operacional", "REGISTRO", context, "SERVIDOR_SECRETARIA", false,
                List.of("REGISTRO", "CERTIDAO"), List.of(context.scopeAxis(), context.branchAxis()), List.of("REGISTRO")));
        rules.add(baseRule("EXECUTAR_RETORNO_PROCESSO", "Executar retorno ao processo", "RETORNO", context, "SERVIDOR_SECRETARIA", context.escalationRequired(),
                List.of("RETORNO", "CUMPRIMENTO"), List.of(context.scopeAxis(), context.branchAxis()), List.of("RETORNO", "CUMPRIMENTO")));
        if (context.sensitive()) {
            rules.add(baseRule("OPERAR_ATO_SIGILOSO", "Operar ato sigiloso com credencial funcional", "SIGILO", context, "SERVIDOR_CREDENCIADO", true,
                    List.of("SIGILO", "ANEXACAO", "CERTIFICACAO"), List.of(context.scopeAxis(), context.branchAxis()), List.of("SIGILO", context.secrecyAxis())));
        }
        if (context.hearingSensitive() || context.recursalOrCollegiate()) {
            rules.add(baseRule("ORGANIZAR_PAUTA_COLEGIADA", "Organizar pauta colegiada e governança de sessão", "PAUTA", context, context.recursalOrCollegiate() ? "ASSESSORIA_COLEGIADO" : "SERVIDOR_SECRETARIA", true,
                    List.of("PAUTA", "SESSAO", "COLEGIADO"), List.of(context.scopeAxis(), context.branchAxis()), List.of("COLEGIADO", "AUDIENCIA")));
        }
        rules.add(baseRule("EXPEDIR_COMUNICACAO_INSTITUCIONAL", "Expedir comunicação institucional quando couber", "EXPEDICAO", context, "SERVIDOR_SECRETARIA", context.sensitive(),
                List.of("EXPEDICAO", "OFICIO", "INTIMACAO"), List.of(context.scopeAxis(), context.branchAxis()), List.of("EXPEDICAO")));
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("ruleCount", rules.size());
        metrics.put("hearingSensitive", context.hearingSensitive());
        metrics.put("sensitive", context.sensitive());
        metrics.put("escalationRequired", context.escalationRequired());
        metrics.put("items", context.itemCount());
        metrics.put("branchAxis", context.branchAxis());
        metrics.put("instanceAxis", context.instanceAxis());
        metrics.put("ramoAxis", context.ramoAxis());
        metrics.put("ritoAxis", context.ritoAxis());
        metrics.put("phaseAxis", context.phaseAxis());
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        if (!Boolean.TRUE.equals(profile.actorScope().institutionBorn())) {
            warnings.add("Secretaria sem âncora institucional completa para ato delegado final.");
        }
        if (context.sensitive()) {
            warnings.add("Ato sensível exige segregação operacional por contato, peça, anexo e certificação.");
        }
        if (context.recursalOrCollegiate()) {
            warnings.add("Contexto recursal/colegiado exige fila, pauta, retorno e credencial reforçados na mesma malha.");
        }
        if (context.itemCount() == 0) {
            warnings.add("Matriz projetada sem itens vivos; revisar descoberta de fila e catálogo formal associado.");
        }
        return new MatrixProjection(context, List.copyOf(rules), Map.copyOf(metrics), List.copyOf(warnings));
    }

    public MatrixProjection resolveInstitutional(String branchCode,
                                                InstitutionalSupportLaneResolver.InstitutionalSupportLaneSnapshot lane,
                                                Processo processo,
                                                Collection<WorkItem> items) {
        OperationalContext context = buildInstitutionalContext(branchCode, lane, processo, items);
        List<CompetenceRule> rules = new ArrayList<>();
        rules.add(baseRule("ORGANIZAR_AGENDA_MEMBRO", "Organizar agenda do membro", "AGENDA", context, "EQUIPE_APOIO", false,
                List.of("AGENDA", "MEMBRO"), List.of(context.scopeAxis(), context.branchAxis()), List.of("AGENDA")));
        rules.add(baseRule("PREPARAR_PRE_PAUTA", "Preparar pré-pauta e dossiê operacional", "PRE_PAUTA", context, "EQUIPE_APOIO", false,
                List.of("PRE_PAUTA", "DOSSIE"), List.of(context.scopeAxis(), context.branchAxis()), List.of("PRE_PAUTA")));
        rules.add(baseRule("CONFIRMAR_COMPARECIMENTO_INSTITUCIONAL", "Confirmar comparecimento institucional", "COMPARECIMENTO", context, "EQUIPE_APOIO", false,
                List.of("COMPARECIMENTO", "AUDIENCIA"), List.of(context.scopeAxis(), context.branchAxis()), List.of("AGENDA", "COMPARECIMENTO")));
        rules.add(baseRule("ENCAMINHAR_MATERIAL_AO_MEMBRO", "Encaminhar material ao membro", "DOSSIE", context, "EQUIPE_APOIO", context.sensitive(),
                List.of("MATERIAL", "DOSSIE"), List.of(context.scopeAxis(), context.branchAxis()), List.of("DOSSIE")));
        if ("MINISTERIO_PUBLICO".equals(context.branchAxis())) {
            rules.add(baseRule("ORGANIZAR_ATUACAO_MP", "Organizar atuação do Ministério Público", "ATUACAO", context, "SECRETARIA_INSTITUCIONAL", context.sensitive(),
                    List.of("MP", "MANIFESTACAO"), List.of("ESTADUAL", "FEDERAL", "ELEITORAL"), List.of("ATUACAO_MP")));
        }
        if ("DEFENSORIA".equals(context.branchAxis())) {
            rules.add(baseRule("ORGANIZAR_ATENDIMENTO_ASSISTIDO", "Organizar atendimento assistido", "ATENDIMENTO", context, "SECRETARIA_INSTITUCIONAL", context.sensitive(),
                    List.of("ASSISTIDO", "ATENDIMENTO"), List.of("ESTADUAL", "FEDERAL"), List.of("ATENDIMENTO")));
        }
        if ("PROCURADORIA".equals(context.branchAxis())) {
            rules.add(baseRule("ORGANIZAR_REPRESENTACAO_ENTE_PUBLICO", "Organizar representação do ente público", "REPRESENTACAO", context, context.scopeAxis().contains("MUNICIPAL") ? "NUCLEO_MUNICIPAL" : "NUCLEO_PROCURADORIA", true,
                    List.of("ENTE_PUBLICO", "REPRESENTACAO"), List.of("MUNICIPAL", "ESTADUAL", "FEDERAL"), List.of("REPRESENTACAO")));
        }
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("ruleCount", rules.size());
        metrics.put("branchAxis", context.branchAxis());
        metrics.put("scopeAxis", context.scopeAxis());
        metrics.put("ramoAxis", context.ramoAxis());
        metrics.put("ritoAxis", context.ritoAxis());
        metrics.put("sensitive", context.sensitive());
        metrics.put("itemCount", context.itemCount());
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        if (context.sensitive()) {
            warnings.add("Confidencialidade institucional exige gradação entre equipe de apoio, membro e credencial funcional reforçada.");
        }
        if (context.itemCount() == 0) {
            warnings.add("Sem work items vivos para o órgão; a competência foi projetada pelo contexto do processo e do ramo institucional.");
        }
        return new MatrixProjection(context, List.copyOf(rules), Map.copyOf(metrics), List.copyOf(warnings));
    }

    private CompetenceRule baseRule(String code,
                                    String label,
                                    String axis,
                                    OperationalContext context,
                                    String minimumRole,
                                    boolean credentialRequired,
                                    List<String> compatibleCategories,
                                    List<String> institutionalScopes,
                                    List<String> signals) {
        String delegatedFunction = switch (axis) {
            case "SIGILO" -> "CREDENCIAL_SIGILO_OPERACIONAL";
            case "RETORNO" -> "CREDENCIAL_RETORNO_PROCESSUAL";
            case "PAUTA" -> "CREDENCIAL_PAUTA_E_SESSAO";
            case "REPRESENTACAO" -> "CREDENCIAL_REPRESENTACAO_INSTITUCIONAL";
            default -> credentialRequired ? "CREDENCIAL_FUNCIONAL_REFORCADA" : "OPERACAO_ASSISTIDA";
        };
        return new CompetenceRule(
                code,
                label,
                axis,
                minimumRole,
                delegatedFunction,
                context.ritoAxis(),
                context.ramoAxis(),
                context.phaseAxis(),
                context.secrecyAxis(),
                context.urgencyAxis(),
                credentialRequired,
                trimCopy(compatibleCategories),
                trimCopy(institutionalScopes),
                trimCopy(signals)
        );
    }

    private OperationalContext buildSecretariatContext(String inboxKey,
                                                       SecretariatInstitutionalVisibilityService.SecretariatInboxInstitutionalProfile profile,
                                                       ForumDeskPortfolioProfile portfolio,
                                                       Collection<SecretariatQueueItem> items,
                                                       Map<Long, Map<String, Object>> metadataByWorkItemId) {
        ContextAccumulator accumulator = new ContextAccumulator();
        if (items != null) {
            for (SecretariatQueueItem item : items) {
                accumulator.accept(item, metadataByWorkItemId == null ? Map.of() : metadataByWorkItemId.get(item.getWorkItemId()));
            }
        }
        String instanceAxis = firstNonBlank(profile.actorScope().instanceClass(), profile.specialization().secretariatInstanceClass(), inferInstanceFromInbox(inboxKey), "PRIMEIRA_INSTANCIA");
        String branchAxis = firstNonBlank(profile.actorScope().branchClass(), profile.specialization().secretariatBranchClass(), "ESTADUAL");
        String scopeAxis = firstNonBlank(profile.specialization().specializedSecretariatCode(), profile.actorScope().specializedSecretariatCode(), portfolio.coordinationDescriptor(), "SECRETARIA_PADRAO");
        return new OperationalContext(
                "SECRETARIAT",
                scopeAxis,
                branchAxis,
                instanceAxis,
                accumulator.resolveRamo(),
                accumulator.resolveRito(),
                accumulator.resolveFase(),
                accumulator.resolveSecrecyAxis(),
                accumulator.resolveUrgencyAxis(),
                accumulator.hearingSensitive,
                accumulator.sensitive,
                accumulator.escalationRequired,
                accumulator.recursalOrCollegiate,
                accumulator.itemCount,
                accumulator.latestReferenceAt
        );
    }

    private OperationalContext buildInstitutionalContext(String branchCode,
                                                         InstitutionalSupportLaneResolver.InstitutionalSupportLaneSnapshot lane,
                                                         Processo processo,
                                                         Collection<WorkItem> items) {
        ContextAccumulator accumulator = new ContextAccumulator();
        if (processo != null) {
            accumulator.accept(processo);
        }
        if (items != null) {
            for (WorkItem item : items) {
                accumulator.accept(item);
            }
        }
        return new OperationalContext(
                "INSTITUTIONAL",
                firstNonBlank(lane.scope(), "ESTADUAL"),
                firstNonBlank(branchCode, lane.branchCode(), "INSTITUTIONAL"),
                inferInstitutionalInstance(lane, processo),
                accumulator.resolveRamo(),
                accumulator.resolveRito(),
                accumulator.resolveFase(),
                accumulator.resolveSecrecyAxis(),
                accumulator.resolveUrgencyAxis(),
                accumulator.hearingSensitive,
                accumulator.sensitive,
                accumulator.escalationRequired,
                accumulator.recursalOrCollegiate,
                accumulator.itemCount,
                accumulator.latestReferenceAt
        );
    }

    private String inferInstitutionalInstance(InstitutionalSupportLaneResolver.InstitutionalSupportLaneSnapshot lane, Processo processo) {
        if (processo != null && processo.getFaseAtual() != null && processo.getFaseAtual().isRecursal()) {
            return "SEGUNDA_INSTANCIA";
        }
        if (lane != null && lane.tribunalCodigo() != null && (lane.tribunalCodigo().startsWith("ST") || lane.tribunalCodigo().startsWith("TSE"))) {
            return "SUPERIOR";
        }
        return "PRIMEIRA_INSTANCIA";
    }

    private String inferInstanceFromInbox(String inboxKey) {
        String normalized = upper(inboxKey);
        if (normalized.contains(":2G:") || normalized.contains(":SECOND:")) {
            return "SEGUNDA_INSTANCIA";
        }
        if (normalized.contains(":SUP:") || normalized.contains(":SUPERIOR:")) {
            return "SUPERIOR";
        }
        return null;
    }

    private static String upper(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static List<String> trimCopy(Collection<String> raw) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (raw != null) {
            for (String item : raw) {
                if (item != null && !item.isBlank()) {
                    out.add(item.trim());
                }
            }
        }
        return List.copyOf(out);
    }

    public record MatrixProjection(
            OperationalContext context,
            List<CompetenceRule> rules,
            Map<String, Object> metrics,
            List<String> warnings
    ) {
    }

    public record CompetenceRule(
            String actCode,
            String actLabel,
            String actAxis,
            String minimumRole,
            String delegatedFunction,
            String ritoAxis,
            String ramoAxis,
            String phaseAxis,
            String secrecyAxis,
            String urgencyAxis,
            boolean functionalCredentialRequired,
            List<String> compatibleCategories,
            List<String> institutionalScopes,
            List<String> signals
    ) {
    }

    public record OperationalContext(
            String domainAxis,
            String scopeAxis,
            String branchAxis,
            String instanceAxis,
            String ramoAxis,
            String ritoAxis,
            String phaseAxis,
            String secrecyAxis,
            String urgencyAxis,
            boolean hearingSensitive,
            boolean sensitive,
            boolean escalationRequired,
            boolean recursalOrCollegiate,
            int itemCount,
            Instant latestReferenceAt
    ) {
    }

    private static final class ContextAccumulator {
        private final Map<String, Integer> ramoVotes = new LinkedHashMap<>();
        private final Map<String, Integer> ritoVotes = new LinkedHashMap<>();
        private final Map<String, Integer> faseVotes = new LinkedHashMap<>();
        private final Map<String, Integer> secrecyVotes = new LinkedHashMap<>();
        private boolean hearingSensitive;
        private boolean sensitive;
        private boolean escalationRequired;
        private boolean recursalOrCollegiate;
        private int itemCount;
        private Instant latestReferenceAt;

        private void accept(SecretariatQueueItem item, Map<String, Object> metadata) {
            if (item == null) {
                return;
            }
            itemCount++;
            hearingSensitive = hearingSensitive || item.isHearingSensitive();
            escalationRequired = escalationRequired || item.isEscalationRequired() || item.isBlocking();
            sensitive = sensitive || item.isSecrecyReviewRequired() || item.isHearingSensitive() || contains(item.getTitulo(), "SIGIL") || contains(item.getQueueCode(), "SIGIL");
            recursalOrCollegiate = recursalOrCollegiate || contains(item.getQueueCode(), "RECURSAL", "COLEGIADO", "ACORDAO", "EMBARG") || contains(item.getTitulo(), "SESSAO", "ACORDAO", "COLEGIADO");
            latestReferenceAt = max(latestReferenceAt, firstNonNull(item.getUpdatedAt(), item.getDueAt(), item.getCreatedAt()));
            vote(ramoVotes, token(metadata, "ramoDireito"));
            vote(ritoVotes, token(metadata, "ritoProcessual"));
            vote(faseVotes, token(metadata, "faseProcessual"));
            vote(secrecyVotes, token(metadata, "nivelSigilo"));
            if (item.isSecrecyReviewRequired()) {
                vote(secrecyVotes, "SIGILO_OPERACIONAL");
            }
            if (item.getPrioridade() != null && item.getPrioridade() <= 1) {
                vote(secrecyVotes, resolveSecrecyByUrgency(false));
            }
        }

        private void accept(WorkItem item) {
            if (item == null) {
                return;
            }
            itemCount++;
            hearingSensitive = hearingSensitive || contains(item.getTitulo(), "AUDIENCIA", "PAUTA", "SESSAO");
            escalationRequired = escalationRequired || item.isBlocking();
            recursalOrCollegiate = recursalOrCollegiate || contains(item.getQueueCode(), "RECURSAL", "COLEGIADO", "ACORDAO") || item.getFaseOrigem() == FaseProcessual.RECURSAL;
            latestReferenceAt = max(latestReferenceAt, firstNonNull(item.getUpdatedAt(), item.getDueAt(), item.getCreatedAt()));
            if (item.getProcesso() != null) {
                accept(item.getProcesso());
            }
        }

        private void accept(Processo processo) {
            if (processo == null) {
                return;
            }
            vote(ramoVotes, processo.getRamoDireito() == null ? null : processo.getRamoDireito().name());
            vote(ritoVotes, processo.getRito() == null ? null : processo.getRito().name());
            vote(faseVotes, processo.getFaseAtual() == null ? null : processo.getFaseAtual().name());
            vote(secrecyVotes, processo.getNivelSigilo() == null ? null : processo.getNivelSigilo().name());
            sensitive = sensitive || (processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO)
                    || (processo.getRamoDireito() != null && (processo.getRamoDireito().geraSigiloAutomatico() || processo.getRamoDireito().isPenalLike()));
            recursalOrCollegiate = recursalOrCollegiate || processo.getFaseAtual() == FaseProcessual.RECURSAL;
        }

        private String resolveRamo() {
            String voted = winner(ramoVotes);
            if (voted != null) {
                return voted;
            }
            String rito = resolveRito();
            return rito == null ? "INDEFINIDO" : inferRamoByRito(rito);
        }

        private String resolveRito() {
            String voted = winner(ritoVotes);
            return voted == null ? "COMUM_ORDINARIO" : voted;
        }

        private String resolveFase() {
            String voted = winner(faseVotes);
            if (voted != null) {
                return voted;
            }
            return recursalOrCollegiate ? FaseProcessual.RECURSAL.name() : FaseProcessual.CONHECIMENTO.name();
        }

        private String resolveSecrecyAxis() {
            if (sensitive) {
                String voted = winner(secrecyVotes);
                if (voted != null) {
                    return voted;
                }
                return "SIGILO_REFORCADO";
            }
            return "PUBLICO_OPERACIONAL";
        }

        private String resolveUrgencyAxis() {
            return escalationRequired ? "CRITICO" : hearingSensitive ? "ALTO" : "PADRAO";
        }

        private static String inferRamoByRito(String rito) {
            RitoProcessual resolved = RitoProcessual.fromString(rito);
            RamoDireito ramo = resolved == null ? null : resolved.suggestedRamo();
            return ramo == null ? "CIVIL" : ramo.name();
        }

        private static String token(Map<String, Object> metadata, String key) {
            if (metadata == null || key == null) {
                return null;
            }
            Object raw = metadata.get(key);
            return raw == null ? null : String.valueOf(raw).trim();
        }

        private static void vote(Map<String, Integer> bucket, String token) {
            if (token == null || token.isBlank()) {
                return;
            }
            bucket.merge(token.trim().toUpperCase(Locale.ROOT), 1, Integer::sum);
        }

        private static String winner(Map<String, Integer> bucket) {
            return bucket.entrySet().stream()
                    .max(Map.Entry.<String, Integer>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                    .map(Map.Entry::getKey)
                    .orElse(null);
        }

        private static boolean contains(String value, String... tokens) {
            if (value == null || value.isBlank() || tokens == null) {
                return false;
            }
            String normalized = value.toUpperCase(Locale.ROOT);
            for (String token : tokens) {
                if (token != null && normalized.contains(token)) {
                    return true;
                }
            }
            return false;
        }

        private static Instant max(Instant first, Instant second) {
            if (first == null) {
                return second;
            }
            if (second == null) {
                return first;
            }
            return Comparator.<Instant>naturalOrder().compare(first, second) >= 0 ? first : second;
        }

        private static Instant firstNonNull(Instant... values) {
            if (values == null) {
                return null;
            }
            for (Instant value : values) {
                if (value != null) {
                    return value;
                }
            }
            return null;
        }

        private static String resolveSecrecyByUrgency(boolean noop) {
            return noop ? "PUBLICO_OPERACIONAL" : "SIGILO_REFORCADO";
        }
    }
}
