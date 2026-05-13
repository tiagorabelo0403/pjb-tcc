package com.tcc.pjb.backend.core.operational;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.Locale;

public final class MagistraturaAccessScopeResolver {

    private MagistraturaAccessScopeResolver() {
    }

    public static AccessScope resolve(TipoUsuario tipoUsuario,
                                      String tribunalCodigo,
                                      String unidadeCodigo,
                                      String caixaCodigo) {
        String tribunalAxis = resolveTribunalAxis(tipoUsuario, tribunalCodigo, unidadeCodigo, caixaCodigo);
        String justiceAxis = computeJusticeAxis(tipoUsuario, tribunalAxis, unidadeCodigo, caixaCodigo);
        String authorityClass = resolveAuthorityClass(tipoUsuario, tribunalAxis);
        String panelCode = resolvePanelCode(authorityClass, justiceAxis, tribunalAxis);
        String landingPath = resolveLandingPath(authorityClass, justiceAxis, tribunalAxis, unidadeCodigo, caixaCodigo);
        return new AccessScope(justiceAxis, tribunalAxis, authorityClass, panelCode, landingPath);
    }

    public static String resolveJusticeAxis(TipoUsuario tipoUsuario,
                                            String tribunalCodigo,
                                            String unidadeCodigo,
                                            String caixaCodigo) {
        return resolve(tipoUsuario, tribunalCodigo, unidadeCodigo, caixaCodigo).justiceAxis();
    }

    public static String resolveTribunalAxis(TipoUsuario tipoUsuario,
                                             String tribunalCodigo,
                                             String unidadeCodigo,
                                             String caixaCodigo) {
        String normalized = normalize(join(tribunalCodigo, unidadeCodigo, caixaCodigo));
        if (tipoUsuario == TipoUsuario.MINISTRO || containsAny(normalized, "STF", "STJ", "TST", "TSE", "STM", "SUPERIOR")) {
            if (containsAny(normalized, "STF", "SUPREMO")) {
                return "STF";
            }
            if (containsAny(normalized, "STJ", "SUPERIOR_TRIBUNAL_DE_JUSTICA")) {
                return "STJ";
            }
            if (containsAny(normalized, "TST", "TRIBUNAL_SUPERIOR_DO_TRABALHO")) {
                return "TST";
            }
            if (containsAny(normalized, "TSE", "TRIBUNAL_SUPERIOR_ELEITORAL")) {
                return "TSE";
            }
            if (containsAny(normalized, "STM", "SUPERIOR_TRIBUNAL_MILITAR")) {
                return "STM";
            }
            return "TRIBUNAL_SUPERIOR";
        }
        if (tipoUsuario == TipoUsuario.DESEMBARGADOR_FEDERAL || containsAny(normalized, "TRF", "TRIBUNAL_REGIONAL_FEDERAL")) {
            return firstNonBlank(extractPrefixedTribunal(normalized, "TRF"), "TRF");
        }
        if (tipoUsuario == TipoUsuario.DESEMBARGADOR || containsAny(normalized, "TJ", "TRIBUNAL_DE_JUSTICA")) {
            return firstNonBlank(extractPrefixedTribunal(normalized, "TJ"), "TJ");
        }
        if (containsAny(normalized, "TRE", "TRIBUNAL_REGIONAL_ELEITORAL")) {
            return firstNonBlank(extractPrefixedTribunal(normalized, "TRE"), "TRE");
        }
        if (containsAny(normalized, "TRT", "TRIBUNAL_REGIONAL_DO_TRABALHO")) {
            return firstNonBlank(extractPrefixedTribunal(normalized, "TRT"), "TRT");
        }
        if (containsAny(normalized, "JF", "SECAO_JUDICIARIA", "SUBSECAO_JUDICIARIA", "JUSTICA_FEDERAL")) {
            return "JF";
        }
        if (containsAny(normalized, "JUIZADO", "JEF")) {
            return "JEF";
        }
        if (containsAny(normalized, "ELEITORAL", "ZE", "ZONA_ELEITORAL")) {
            return "JUSTICA_ELEITORAL";
        }
        if (containsAny(normalized, "TRABALHO", "VT", "VARA_DO_TRABALHO")) {
            return "JUSTICA_TRABALHO";
        }
        if (containsAny(normalized, "MILITAR")) {
            return "JUSTICA_MILITAR";
        }
        return tribunalCodigo == null || tribunalCodigo.isBlank() ? "TJ" : tribunalCodigo.trim().toUpperCase(Locale.ROOT);
    }

    public static String resolveAuthorityClass(TipoUsuario tipoUsuario, String tribunalAxis) {
        if (tipoUsuario == TipoUsuario.JUIZ || tipoUsuario == TipoUsuario.JUIZ_ESTADUAL || tipoUsuario == TipoUsuario.JUIZ_FEDERAL
                || tipoUsuario == TipoUsuario.JUIZ_TRABALHISTA || tipoUsuario == TipoUsuario.JUIZ_ELEITORAL
                || tipoUsuario == TipoUsuario.JUIZ_MILITAR || tipoUsuario == TipoUsuario.JUIZ_ESPECIAL) {
            return "JUIZ";
        }
        if (tipoUsuario == TipoUsuario.MINISTRO || containsAny(tribunalAxis, "STF", "STJ", "TST", "TSE", "STM", "TRIBUNAL_SUPERIOR")) {
            return "MINISTRO";
        }
        if (tipoUsuario == TipoUsuario.DESEMBARGADOR || tipoUsuario == TipoUsuario.DESEMBARGADOR_FEDERAL
                || containsAny(tribunalAxis, "TJ", "TRF", "TRE", "TRT")) {
            return "DESEMBARGADOR";
        }
        return "JUIZ";
    }

    private static String computeJusticeAxis(TipoUsuario tipoUsuario,
                                             String tribunalAxis,
                                             String unidadeCodigo,
                                             String caixaCodigo) {
        String normalized = normalize(join(tribunalAxis, unidadeCodigo, caixaCodigo));
        return switch (tipoUsuario == null ? TipoUsuario.MAGISTRADO : tipoUsuario) {
            case MINISTRO -> "SUPERIOR";
            case DESEMBARGADOR_FEDERAL, JUIZ_FEDERAL -> "FEDERAL";
            case JUIZ_TRABALHISTA -> "TRABALHO";
            case JUIZ_ELEITORAL -> "ELEITORAL";
            case JUIZ_MILITAR -> "MILITAR";
            case JUIZ_ESPECIAL -> "ESPECIAL";
            case DESEMBARGADOR, JUIZ, JUIZ_ESTADUAL, MAGISTRADO -> inferFromTokens(normalized);
            default -> inferFromTokens(normalized);
        };
    }

    private static String inferFromTokens(String normalized) {
        if (containsAny(normalized, "STF", "STJ", "TST", "TSE", "STM", "SUPERIOR")) {
            return "SUPERIOR";
        }
        if (containsAny(normalized, "TRF", "JF", "JUSTICA_FEDERAL", "SECAO_JUDICIARIA", "SUBSECAO_JUDICIARIA", "JEF")) {
            return containsAny(normalized, "JEF") ? "ESPECIAL_FEDERAL" : "FEDERAL";
        }
        if (containsAny(normalized, "TRE", "ELEITORAL", "ZONA_ELEITORAL")) {
            return "ELEITORAL";
        }
        if (containsAny(normalized, "TRT", "TRABALHO", "VARA_DO_TRABALHO")) {
            return "TRABALHO";
        }
        if (containsAny(normalized, "MILITAR")) {
            return "MILITAR";
        }
        if (containsAny(normalized, "JUIZADO", "JEFAZ", "JEC", "JEF")) {
            return "ESPECIAL";
        }
        return "ESTADUAL";
    }

    private static String resolvePanelCode(String authorityClass,
                                           String justiceAxis,
                                           String tribunalAxis) {
        String tribunalSuffix = tribunalAxis == null || tribunalAxis.isBlank() ? "" : '_' + tribunalAxis.replace('-', '_');
        return switch (authorityClass) {
            case "MINISTRO" -> "MAGISTRATURA_MINISTRO" + tribunalSuffix;
            case "DESEMBARGADOR" -> "MAGISTRATURA_DESEMBARGADOR_" + justiceAxis + tribunalSuffix;
            default -> "MAGISTRATURA_JUIZ_" + justiceAxis + tribunalSuffix;
        };
    }

    private static String resolveLandingPath(String authorityClass,
                                             String justiceAxis,
                                             String tribunalAxis,
                                             String unidadeCodigo,
                                             String caixaCodigo) {
        String orgao = firstNonBlank(caixaCodigo, unidadeCodigo);
        return switch (authorityClass) {
            case "MINISTRO" -> OperationalApiRoutes.ministroPlenarioPainel(tribunalAxis, unidadeCodigo, caixaCodigo, orgao);
            case "DESEMBARGADOR" -> OperationalApiRoutes.desembargadorPainel(justiceAxis, tribunalAxis, unidadeCodigo, caixaCodigo, orgao);
            default -> OperationalApiRoutes.judgeGabinetePainel(justiceAxis, tribunalAxis, unidadeCodigo, caixaCodigo, orgao);
        };
    }

    private static String extractPrefixedTribunal(String normalized, String prefix) {
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        for (String token : normalized.split("[^A-Z0-9]+")) {
            if (token.startsWith(prefix)) {
                return token;
            }
        }
        return null;
    }

    private static String join(String... values) {
        StringBuilder sb = new StringBuilder();
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append(' ');
                }
                sb.append(value.trim());
            }
        }
        return sb.toString();
    }

    private static boolean containsAny(String haystack, String... needles) {
        if (haystack == null || haystack.isBlank() || needles == null) {
            return false;
        }
        String normalized = normalize(haystack);
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && normalized.contains(normalize(needle))) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT)
                .replace('Á', 'A').replace('À', 'A').replace('Ã', 'A').replace('Â', 'A')
                .replace('É', 'E').replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O').replace('Õ', 'O').replace('Ô', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C');
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

    public record AccessScope(
            String justiceAxis,
            String tribunalAxis,
            String authorityClass,
            String panelCode,
            String landingPath
    ) {
    }
}
