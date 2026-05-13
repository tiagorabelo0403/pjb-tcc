package com.tcc.pjb.backend.service.institutional.support.lane;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.security.operational.OperationalFunctionCredentialAuthorityService;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InstitutionalSupportLaneResolver {

    private static final Pattern TRIBUNAL_PATTERN = Pattern.compile("\\b(TJ[A-Z]{2}|TRF\\d|TRT\\d+|TRE[-_]?[A-Z]{2}|TSE|STJ|STF|TST|STM|TJM[A-Z]{0,2})\\b");

    private final CurrentUserService currentUserService;
    private final OperationalFunctionCredentialAuthorityService authorityService;

    public InstitutionalSupportLaneResolver(CurrentUserService currentUserService,
                                            OperationalFunctionCredentialAuthorityService authorityService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.authorityService = Objects.requireNonNull(authorityService);
    }

    public InstitutionalSupportLaneSnapshot requireCurrentUser() {
        return require(currentUserService.getRequired());
    }

    public InstitutionalSupportLaneSnapshot require(Usuario usuario) {
        InstitutionalSupportLaneSnapshot snapshot = resolve(usuario);
        if (snapshot == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "usuário não pertence a uma secretaria institucional provisionável");
        }
        return snapshot;
    }

    public InstitutionalSupportLaneSnapshot resolve(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        TipoUsuario tipoUsuario = usuario.getTipoUsuario();
        BranchProjection projection = resolveBranch(usuario, tipoUsuario);
        if (projection == null) {
            return null;
        }
        String tribunalCodigo = firstNonBlank(authorityService.resolveTribunal(usuario), detectTribunal(usuario), projection.defaultTribunalCode());
        String scope = projection.scope(tribunalCodigo);
        String normalizedUf = normalizeNullable(usuario.getUf());
        String normalizedComarca = normalizeNullable(usuario.getComarca());
        String officePrefix = projection.inboxPrefix(scope, tribunalCodigo);
        String forumAnchor = normalizedComarca == null ? null : normalizedComarca.replace('_', '-');
        String memberPanelPath = projection.memberPanelPath();
        return new InstitutionalSupportLaneSnapshot(
                projection.branchCode(),
                projection.branchLabel(),
                scope,
                projection.federativeAxis(scope),
                tribunalCodigo,
                normalizedUf,
                normalizedComarca,
                officePrefix,
                forumAnchor,
                memberPanelPath,
                projection.snapshotPath(),
                projection.credentialBasePath(),
                projection.actorRoles(),
                projection.capabilities(),
                projection.warnings(scope, tribunalCodigo, normalizedComarca)
        );
    }

    private BranchProjection resolveBranch(Usuario usuario, TipoUsuario tipoUsuario) {
        if (tipoUsuario != null) {
            if (tipoUsuario.isMinisterioPublico()) {
                return BranchProjection.ministerioPublico(tipoUsuario);
            }
            if (tipoUsuario.isDefensoriaPublica()) {
                return BranchProjection.defensoria(tipoUsuario);
            }
            if (tipoUsuario.isProcuradoria()) {
                return BranchProjection.procuradoria(tipoUsuario);
            }
        }
        Set<String> tokens = collectTokens(usuario);
        if (tokens.isEmpty()) {
            return null;
        }
        if (containsAny(tokens, "PROMOTORIA", "MINISTERIO_PUBLICO", "MPE", "MPF", "MPT", "PGR", "MP")) {
            return BranchProjection.ministerioPublico(tipoUsuario);
        }
        if (containsAny(tokens, "DEFENSORIA", "DEFENSORIA_PUBLICA", "DPE", "DPU")) {
            return BranchProjection.defensoria(tipoUsuario);
        }
        if (containsAny(tokens, "PROCURADORIA", "PGM", "PGE", "AGU", "PGF", "PGFN")) {
            return BranchProjection.procuradoria(tipoUsuario);
        }
        return null;
    }

    private String detectTribunal(Usuario usuario) {
        for (String token : collectTokens(usuario)) {
            Matcher matcher = TRIBUNAL_PATTERN.matcher(token);
            if (matcher.find()) {
                return normalizeNullable(matcher.group(1));
            }
        }
        return null;
    }

    private Set<String> collectTokens(Usuario usuario) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (usuario == null) {
            return out;
        }
        collect(out, usuario.getPerfil());
        collect(out, usuario.getRegistroProfissional());
        collect(out, usuario.getUf());
        collect(out, usuario.getComarca());
        collect(out, usuario.getEmail());
        if (usuario.getEspecialidades() != null) {
            for (String item : usuario.getEspecialidades()) {
                collect(out, item);
            }
        }
        return out;
    }

    private void collect(Set<String> out, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String normalized = normalizeNullable(raw);
        if (normalized == null) {
            return;
        }
        out.add(normalized);
        for (String part : normalized.split("[_:>\\-\\s@.]+")) {
            if (!part.isBlank()) {
                out.add(part);
            }
        }
    }

    private boolean containsAny(Set<String> tokens, String... expected) {
        if (tokens == null || tokens.isEmpty() || expected == null) {
            return false;
        }
        for (String candidate : expected) {
            String normalized = normalizeNullable(candidate);
            if (normalized != null && tokens.contains(normalized)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeNullable(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        normalized = normalized.replaceAll("[^A-Za-z0-9]+", "_").replaceAll("_+", "_").replaceAll("^_+|_+$", "");
        if (normalized.isBlank()) {
            return null;
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String firstNonBlank(String... values) {
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

    public record InstitutionalSupportLaneSnapshot(
            String branchCode,
            String branchLabel,
            String scope,
            String federativeAxis,
            String tribunalCodigo,
            String uf,
            String comarca,
            String inboxPrefix,
            String forumAnchor,
            String memberPanelPath,
            String snapshotPath,
            String credentialBasePath,
            List<String> actorRoles,
            List<String> capabilities,
            List<String> warnings
    ) {
    }

    private record BranchProjection(
            String branchCode,
            String branchLabel,
            String defaultScope,
            String defaultTribunalCode,
            String inboxAxis,
            String snapshotPath,
            String credentialBasePath,
            String memberPanelPath,
            List<String> actorRoles,
            List<String> capabilities
    ) {
        static BranchProjection ministerioPublico(TipoUsuario tipoUsuario) {
            String scope = switch (tipoUsuario) {
                case PROMOTOR_TRABALHISTA, PROCURADOR_GERAL_REPUBLICA -> "FEDERAL";
                case PROMOTOR_ELEITORAL -> "ELEITORAL";
                default -> "ESTADUAL";
            };
            return new BranchProjection(
                    "MINISTERIO_PUBLICO",
                    "Secretaria institucional do Ministério Público",
                    scope,
                    "TJ",
                    "MP",
                    OperationalApiRoutes.institutionalSupportSnapshot("MINISTERIO_PUBLICO"),
                    OperationalApiRoutes.institutionalSupportCredentialSecurity("MINISTERIO_PUBLICO"),
                    "/api/v1/mp/painel",
                    List.of("MEMBRO_MINISTERIO_PUBLICO", "PROMOTOR_ELEITORAL", "PROMOTOR_TRABALHISTA", "PROCURADOR_GERAL_REPUBLICA"),
                    List.of("AGENDA_AUDIENCIA", "AGENDA_SESSAO", "ORGANIZACAO_INTIMACOES", "MALHA_INSTITUCIONAL")
            );
        }

        static BranchProjection defensoria(TipoUsuario tipoUsuario) {
            String scope = tipoUsuario == TipoUsuario.DEFENSOR_PUBLICO_FEDERAL ? "FEDERAL" : "ESTADUAL";
            return new BranchProjection(
                    "DEFENSORIA",
                    "Secretaria institucional da Defensoria",
                    scope,
                    scope.equals("FEDERAL") ? "TRF" : "TJ",
                    "DEFENSORIA",
                    OperationalApiRoutes.institutionalSupportSnapshot("DEFENSORIA"),
                    OperationalApiRoutes.institutionalSupportCredentialSecurity("DEFENSORIA"),
                    "/api/v1/defensor/painel",
                    List.of("DEFENSOR_PUBLICO", "DEFENSOR_PUBLICO_FEDERAL"),
                    List.of("AGENDA_AUDIENCIA", "AGENDA_SESSAO", "ORGANIZACAO_ASSISTIDOS", "MALHA_INSTITUCIONAL")
            );
        }

        static BranchProjection procuradoria(TipoUsuario tipoUsuario) {
            String scope = switch (tipoUsuario) {
                case PROCURADORIA_MUNICIPAL -> "MUNICIPAL";
                case PROCURADORIA_ESTADUAL -> "ESTADUAL";
                default -> "FEDERAL";
            };
            return new BranchProjection(
                    "PROCURADORIA",
                    "Secretaria institucional da Procuradoria",
                    scope,
                    scope.equals("FEDERAL") ? "TRF" : "TJ",
                    "PROC",
                    OperationalApiRoutes.institutionalSupportSnapshot("PROCURADORIA"),
                    OperationalApiRoutes.institutionalSupportCredentialSecurity("PROCURADORIA"),
                    "/api/v1/procuradoria/operacional/snapshot",
                    List.of("PROCURADOR", "PROCURADORIA_MUNICIPAL", "PROCURADORIA_ESTADUAL", "PROCURADORIA_FEDERAL"),
                    List.of("AGENDA_AUDIENCIA", "AGENDA_SESSAO", "ORGANIZACAO_REPRESENTACAO", "MALHA_INSTITUCIONAL")
            );
        }

        String scope(String tribunalCodigo) {
            if ("FEDERAL".equals(defaultScope) || "MUNICIPAL".equals(defaultScope) || "ELEITORAL".equals(defaultScope)) {
                return defaultScope;
            }
            if (tribunalCodigo != null && tribunalCodigo.startsWith("TRF")) {
                return "FEDERAL";
            }
            return defaultScope;
        }

        String federativeAxis(String resolvedScope) {
            return switch (resolvedScope) {
                case "MUNICIPAL" -> "MUNICIPAL";
                case "FEDERAL", "ELEITORAL" -> "FEDERAL";
                default -> "ESTADUAL";
            };
        }

        String inboxPrefix(String scope, String tribunalCodigo) {
            return inboxAxis + ':' + firstNonBlank(scope, defaultScope) + ':' + firstNonBlank(tribunalCodigo, defaultTribunalCode);
        }

        List<String> warnings(String scope, String tribunalCodigo, String comarca) {
            ArrayList<String> warnings = new ArrayList<>();
            if (tribunalCodigo == null || tribunalCodigo.isBlank()) {
                warnings.add("Tribunal base ausente no perfil institucional; a triagem usa prefixo institucional amplo.");
            }
            if (comarca == null || comarca.isBlank()) {
                warnings.add("Comarca ausente no perfil institucional; a agenda poderá abranger múltiplas sedes do mesmo órgão.");
            }
            if ("ELEITORAL".equals(scope)) {
                warnings.add("Secretaria ministerial eleitoral opera em malha federal-eleitoral segregada.");
            }
            return List.copyOf(new LinkedHashSet<>(warnings));
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
    }
}
