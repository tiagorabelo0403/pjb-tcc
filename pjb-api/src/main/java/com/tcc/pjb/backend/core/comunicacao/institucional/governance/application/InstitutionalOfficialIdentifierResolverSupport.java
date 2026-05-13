package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class InstitutionalOfficialIdentifierResolverSupport {

    private static final Pattern DIGITS_7 = Pattern.compile("\\b(\\d{7})\\b");

    private InstitutionalOfficialIdentifierResolverSupport() {
    }

    static String normalizeCnpj(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        return digits.length() == 14 ? digits : null;
    }

    static boolean isValidCnpj(String raw) {
        String digits = normalizeCnpj(raw);
        if (digits == null || digits.chars().distinct().count() == 1) {
            return false;
        }
        return computeCnpjDigit(digits, 12) == Character.getNumericValue(digits.charAt(12))
                && computeCnpjDigit(digits, 13) == Character.getNumericValue(digits.charAt(13));
    }

    static String deriveIbgeMunicipioCode(String comarca, String unidadeCodigo, List<String> abrangenciasTerritoriais) {
        String direct = findSevenDigits(join(abrangenciasTerritoriais));
        if (direct != null) {
            return direct;
        }
        direct = findSevenDigits(comarca);
        if (direct != null) {
            return direct;
        }
        return findSevenDigits(unidadeCodigo);
    }

    static String deriveDataJudAlias(String orgaoSigla) {
        String normalized = normalizeToken(orgaoSigla);
        if (normalized == null) {
            return null;
        }
        if (!normalized.startsWith("TJ")
                && !normalized.startsWith("TRF")
                && !normalized.startsWith("TRT")
                && !normalized.startsWith("TRE")
                && !normalized.startsWith("TST")
                && !normalized.startsWith("TSE")
                && !normalized.startsWith("STJ")
                && !normalized.startsWith("STM")
                && !normalized.startsWith("STF")
                && !normalized.startsWith("TJM")) {
            return null;
        }
        return "api_publica_" + normalized.toLowerCase(Locale.ROOT);
    }

    static boolean isJudiciaryScope(InstitutionalOrganizationScope scope, String orgaoSigla) {
        if (scope != null) {
            return switch (scope) {
                case FORUM,
                        SECRETARIA_UNIDADE_JUDICIARIA,
                        CENTRAL_AUDIENCIAS,
                        CENTRAL_MANDADOS,
                        CEJUSC,
                        CONTADORIA,
                        EQUIPE_PSICOSSOCIAL,
                        CARTORIO_INTEGRADO -> true;
                default -> false;
            };
        }
        String normalized = normalizeToken(orgaoSigla);
        return normalized != null && (normalized.startsWith("TJ") || normalized.startsWith("TRF") || normalized.startsWith("TRT") || normalized.startsWith("TRE") || normalized.startsWith("ST"));
    }

    static boolean isFederalExecutiveScope(String esferaAdministrativa, InstitutionalOrganizationScope scope) {
        String normalized = normalizeToken(esferaAdministrativa);
        if (normalized == null || !normalized.contains("FEDERAL")) {
            return false;
        }
        return scope == null || scope != InstitutionalOrganizationScope.ORGAO_TECNICO_CONVENIADO;
    }

    static String deriveSiorgUnitCode(String unidadeCodigo) {
        if (unidadeCodigo == null || unidadeCodigo.isBlank()) {
            return null;
        }
        String digits = unidadeCodigo.replaceAll("\\D", "");
        if (digits.length() < 3 || digits.length() > 12) {
            return null;
        }
        return digits;
    }

    static boolean isOfficialInstitutionalDomain(String domain) {
        String normalized = normalizeDomain(domain);
        if (normalized == null) {
            return false;
        }
        return normalized.endsWith(".gov.br")
                || normalized.endsWith(".jus.br")
                || normalized.endsWith(".mp.br")
                || normalized.endsWith(".def.br")
                || normalized.endsWith(".leg.br")
                || normalized.endsWith(".tc.br");
    }

    static String normalizeDomain(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("http://")) {
            normalized = normalized.substring(7);
        }
        if (normalized.startsWith("https://")) {
            normalized = normalized.substring(8);
        }
        if (normalized.startsWith("www.")) {
            normalized = normalized.substring(4);
        }
        int slash = normalized.indexOf('/');
        if (slash >= 0) {
            normalized = normalized.substring(0, slash);
        }
        int at = normalized.indexOf('@');
        if (at >= 0) {
            normalized = normalized.substring(at + 1);
        }
        return normalized.isBlank() ? null : normalized;
    }

    static String buildCnpjLookupUrl(String cnpj) {
        String digits = normalizeCnpj(cnpj);
        return digits == null ? null : "https://solucoes.receita.fazenda.gov.br/Servicos/cnpjreva/Cnpjreva_Solicitacao.asp?cnpj=" + digits;
    }

    static String buildIbgeLookupUrl(String codigoIbge) {
        return codigoIbge == null || codigoIbge.isBlank() ? null : "https://servicodados.ibge.gov.br/api/v1/localidades/municipios/" + codigoIbge;
    }

    static String buildDataJudLookupUrl(String alias) {
        return alias == null || alias.isBlank() ? null : "https://api-publica.datajud.cnj.jus.br/" + alias + "/_search";
    }

    static String buildSiorgLookupUrl(String codigoUnidade) {
        return codigoUnidade == null || codigoUnidade.isBlank() ? null : "https://estruturaorganizacional.dados.gov.br/doc/unidade-organizacional/" + codigoUnidade + "/estrutura.json";
    }

    private static int computeCnpjDigit(String digits, int position) {
        int[] weights = position == 12
                ? new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}
                : new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < position; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static String findSevenDigits(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Matcher matcher = DIGITS_7.matcher(raw);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join(" ", values);
    }

    private static String normalizeToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String upper = raw.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return upper.isBlank() ? null : upper;
    }
}
