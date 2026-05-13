package com.tcc.pjb.backend.service.processual.calculo;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CalculoJudicialDomainSupport {

    private static final String BASE_PATH = "/api/v1/processual/calculos";
    private static final String LEGACY_TRABALHISTA_BASE_PATH = "/api/v1/processual/trabalhista";
    private static final List<String> SUPPORTED = List.of("TRABALHISTA_CLT", "FAZENDA_TRIBUTARIO", "CUSTAS_PROCESSUAIS", "FEDERAL_PREVIDENCIARIO_CJF");

    private CalculoJudicialDomainSupport() {
    }

    public static String normalize(String dominio) {
        if (dominio == null || dominio.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(dominio.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('-', '_')
                .replace('/', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT)
                .replaceAll("_+", "_");
        return switch (normalized) {
            case "TRABALHISTA", "TRABALHISTA_CLT" -> "TRABALHISTA_CLT";
            case "FAZENDA", "FAZENDA_TRIBUTARIO", "FAZENDA_E_TRIBUTARIO", "TRIBUTARIO", "DIREITO_TRIBUTARIO", "CALCULO_TRIBUTARIO", "TRIBUTARIO_FAZENDA", "FAZENDARIO" -> "FAZENDA_TRIBUTARIO";
            case "CUSTAS", "CUSTAS_PROCESSUAIS", "CUSTAS_E_DESPESAS", "CUSTAS_DEPOSITOS" -> "CUSTAS_PROCESSUAIS";
            case "FEDERAL_PREVIDENCIARIO", "FEDERAL_PREVIDENCIARIO_CJF", "PREVIDENCIARIO_FEDERAL", "PREVIDENCIARIO", "PREVIDENCIARIO_CJF", "JEF_PREVIDENCIARIO", "CJF_PREVIDENCIARIO" -> "FEDERAL_PREVIDENCIARIO_CJF";
            default -> normalized;
        };
    }

    public static boolean matches(String filtro, String dominioCanonico, String aba) {
        String normalizedFilter = normalize(filtro);
        if (normalizedFilter.isBlank()) {
            return true;
        }
        return normalizedFilter.equals(normalize(dominioCanonico))
                || normalizedFilter.equals(normalize(aba))
                || normalizedFilter.equals(normalize(slug(dominioCanonico)));
    }

    public static boolean isSupported(String dominio) {
        return SUPPORTED.contains(normalize(dominio));
    }

    public static List<String> supportedDomains() {
        return SUPPORTED;
    }

    public static List<String> aliases(String dominio) {
        return switch (normalize(dominio)) {
            case "TRABALHISTA_CLT" -> List.of("trabalhista", "trabalhista-clt", "trabalhista_clt");
            case "FAZENDA_TRIBUTARIO" -> List.of("fazenda", "fazenda-tributario", "fazenda_tributario", "tributario", "tributário", "direito-tributario", "calculo-tributario", "fazenda e tributario");
            case "CUSTAS_PROCESSUAIS" -> List.of("custas", "custas-processuais", "custas_processuais", "custas e despesas", "custas-depositos", "custas_depositos");
            case "FEDERAL_PREVIDENCIARIO_CJF" -> List.of("federal-previdenciario-cjf", "federal_previdenciario_cjf", "previdenciario", "previdenciario-cjf", "previdenciario-federal", "previdenciario_federal", "jef-previdenciario", "cjf-previdenciario");
            default -> List.of();
        };
    }

    public static String requireSupported(String dominio) {
        String normalized = normalize(dominio);
        if (!SUPPORTED.contains(normalized)) {
            throw new CalculoJudicialUnsupportedDomainException(dominio, normalized, SUPPORTED, suggestedDomain(dominio));
        }
        return normalized;
    }

    public static Map<String, String> apiRoutes(String dominio) {
        String canonical = requireSupported(dominio);
        Map<String, String> routes = new LinkedHashMap<>();
        routes.put("workspace", workspaceRoute(canonical));
        routes.put("manualWorkspace", workspaceRoute(canonical));
        routes.put("ajuda", helpRoute(canonical));
        routes.put("assistente", assistenteRoute(canonical));
        routes.put("manualAssistente", assistenteRoute(canonical));
        routes.put("iaFinanceira", financialAiRoute(canonical));
        routes.put("iaFinanceiraExecute", financialAiExecuteRoute());
        routes.put("json", jsonRoute(canonical));
        routes.put("manualJson", jsonRoute(canonical));
        routes.put("pdf", pdfRoute(canonical));
        routes.put("manualPdf", pdfRoute(canonical));
        routes.put("catalogo", catalogRoute(canonical));
        routes.put("bootstrap", bootstrapRoute(canonical));
        routes.put("tabelasOficiais", officialTablesRoute(canonical));
        routes.put("referenciasEconomicas", economicReferencesRoute());
        routes.put("experiencePreference", experiencePreferenceRoute());
        routes.put("experiencePreferenceByDomain", experiencePreferenceRoute(canonical));
        routes.put("liveAjuizamentoAssist", financialAiLiveFilingRoute());
        if ("TRABALHISTA_CLT".equals(canonical)) {
            routes.put("legacyVerbasRescisorias", legacyTrabalhistaVerbasRescisoriasRoute());
        }
        return Map.copyOf(routes);
    }

    public static String slug(String dominio) {
        return switch (normalize(dominio)) {
            case "TRABALHISTA_CLT" -> "trabalhista-clt";
            case "FAZENDA_TRIBUTARIO" -> "fazenda-tributario";
            case "CUSTAS_PROCESSUAIS" -> "custas-processuais";
            case "FEDERAL_PREVIDENCIARIO_CJF" -> "federal-previdenciario-cjf";
            default -> normalize(dominio).toLowerCase(Locale.ROOT).replace('_', '-');
        };
    }

    public static String filenamePrefix(String dominio) {
        return switch (normalize(dominio)) {
            case "TRABALHISTA_CLT" -> "pjb-calculo-trabalhista-clt";
            case "FAZENDA_TRIBUTARIO" -> "pjb-calculo-fazenda-tributario";
            case "CUSTAS_PROCESSUAIS" -> "pjb-calculo-custas-processuais";
            case "FEDERAL_PREVIDENCIARIO_CJF" -> "pjb-calculo-federal-previdenciario-cjf";
            default -> "pjb-calculo-judicial";
        };
    }

    public static String basePath() {
        return BASE_PATH;
    }

    public static String legacyTrabalhistaBasePath() {
        return LEGACY_TRABALHISTA_BASE_PATH;
    }

    public static String legacyTrabalhistaVerbasRescisoriasRoute() {
        return LEGACY_TRABALHISTA_BASE_PATH + "/verbas-rescisorias";
    }

    public static Map<String, Object> routePolicy(String dominio) {
        String canonical = requireSupported(dominio);
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("preferredApiFamily", "calculos");
        policy.put("preferredWorkspaceRoute", workspaceRoute(canonical));
        policy.put("preferredManualWorkspaceRoute", workspaceRoute(canonical));
        policy.put("preferredJsonRoute", jsonRoute(canonical));
        policy.put("preferredManualJsonRoute", jsonRoute(canonical));
        policy.put("preferredPdfRoute", pdfRoute(canonical));
        policy.put("preferredManualPdfRoute", pdfRoute(canonical));
        policy.put("preferredAssistenteRoute", assistenteRoute(canonical));
        policy.put("preferredManualAssistenteRoute", assistenteRoute(canonical));
        policy.put("preferredIaFinanceiraRoute", financialAiExecuteRoute());
        policy.put("preferredIaFinanceiraDomainRoute", financialAiRoute(canonical));
        policy.put("preferredLiveAjuizamentoAssistRoute", financialAiLiveFilingRoute());
        policy.put("catalogRoute", catalogRoute(canonical));
        policy.put("experiencePreferenceRoute", experiencePreferenceRoute());
        policy.put("preferredDomainExperiencePreferenceRoute", experiencePreferenceRoute(canonical));
        policy.put("experiencePreferenceContextFields", List.of("ramoDireito", "classeProcessual", "tipoCausa", "perfilEquipe", "tribunal", "sistemaOrigem"));
        policy.put("institutionalPolicyDimensions", List.of("dominio", "ramoDireito", "classeProcessual", "tipoCausa", "perfilEquipe", "tribunal", "sistemaOrigem"));
        policy.put("preferredContextualBootstrapRoute", bootstrapRoute(canonical));
        policy.put("bootstrapRoute", bootstrapRoute(canonical));
        policy.put("officialTablesRoute", officialTablesRoute(canonical));
        policy.put("compatibilityMode", "canonical_first");
        policy.put("experienceModes", List.of(
                Map.of(
                        "code", "manual_tradicional",
                        "title", "Versão manual",
                        "entryRoute", workspaceRoute(canonical),
                        "assistenteRoute", assistenteRoute(canonical),
                        "jsonRoute", jsonRoute(canonical),
                        "pdfRoute", pdfRoute(canonical),
                        "iaRequired", Boolean.FALSE
                ),
                Map.of(
                        "code", "assistido_com_ia",
                        "title", "Versão com IA",
                        "entryRoute", financialAiExecuteRoute(),
                        "liveAjuizamentoRoute", financialAiLiveFilingRoute(),
                        "domainRoute", financialAiRoute(canonical),
                        "iaRequired", Boolean.TRUE
                )
        ));
        policy.put("iaFinanceiraCompatibilityRoutes", List.of(Map.of(
                "code", "IA_FINANCEIRA_DOMAIN_ROUTE_COMPAT",
                "route", financialAiRoute(canonical),
                "status", "compatibility",
                "frontendUse", "prefer_execute_route",
                "migrationTarget", financialAiExecuteRoute()
        )));
        if ("TRABALHISTA_CLT".equals(canonical)) {
            policy.put("legacyRoutes", List.of(Map.of(
                    "code", "TRABALHISTA_VERBAS_RESCISORIAS_LEGACY",
                    "route", legacyTrabalhistaVerbasRescisoriasRoute(),
                    "status", "compatibility",
                    "frontendUse", "avoid_new_integrations",
                    "migrationTarget", jsonRoute(canonical)
            )));
        } else {
            policy.put("legacyRoutes", List.of());
        }
        if ("FEDERAL_PREVIDENCIARIO_CJF".equals(canonical)) {
            policy.put("benchmarkFamily", "federal_jef_cjf_previdenciario");
        }
        return Map.copyOf(policy);
    }

    public static String workspaceRoute() {
        return BASE_PATH + "/workspace";
    }

    public static String workspaceRoute(String dominio) {
        return workspaceRoute() + "/" + slug(requireSupported(dominio));
    }

    public static String helpRoute(String dominio) {
        return workspaceRoute(dominio) + "/ajuda";
    }

    public static String assistenteRoute(String dominio) {
        return BASE_PATH + "/assistente/" + slug(requireSupported(dominio));
    }

    public static String jsonRoute(String dominio) {
        return BASE_PATH + "/" + slug(requireSupported(dominio));
    }

    public static String financialAiBaseRoute() {
        return BASE_PATH + "/ia/financeira";
    }

    public static String financialAiExecuteRoute() {
        return financialAiBaseRoute() + "/executar";
    }

    public static String financialAiLiveFilingRoute() {
        return financialAiBaseRoute() + "/sinalizar-ajuizamento";
    }

    public static String economicReferencesRoute() {
        return BASE_PATH + "/referencias/economicas";
    }

    public static String experiencePreferenceRoute() {
        return BASE_PATH + "/experiencia/preferencia";
    }

    public static String experiencePreferenceRoute(String dominio) {
        String canonical = dominio == null || dominio.isBlank() ? null : requireSupported(dominio);
        return canonical == null ? experiencePreferenceRoute() : experiencePreferenceRoute() + "?dominio=" + slug(canonical);
    }

    public static String financialAiRoute(String dominio) {
        return financialAiBaseRoute() + "/" + slug(requireSupported(dominio));
    }

    public static String recursalAiRoute() {
        return "/api/v1/processual/recursal/ia/conferencia";
    }

    public static String recursalAdmissibilityRoute() {
        return "/api/v1/processual/recursal/admissibilidade/avaliar";
    }

    public static String pdfRoute(String dominio) {
        return jsonRoute(requireSupported(dominio)) + "/pdf";
    }

    public static String catalogRoute() {
        return BASE_PATH + "/catalogo";
    }

    public static String catalogRoute(String dominio) {
        return catalogRoute() + "/" + slug(requireSupported(dominio));
    }

    public static String bootstrapRoute(String dominio) {
        return catalogRoute(requireSupported(dominio)) + "/bootstrap";
    }


    public static String officialTablesRoute() {
        return BASE_PATH + "/tabelas/oficiais";
    }

    public static String officialTablesRoute(String dominio) {
        return officialTablesRoute() + "/" + slug(requireSupported(dominio));
    }

    public static String aba(String dominio) {
        return switch (normalize(dominio)) {
            case "TRABALHISTA_CLT" -> "Trabalhista CLT";
            case "FAZENDA_TRIBUTARIO" -> "Fazenda e Tributário";
            case "CUSTAS_PROCESSUAIS" -> "Custas e Despesas";
            case "FEDERAL_PREVIDENCIARIO_CJF" -> "Federal/JEF Previdenciário";
            default -> "Calculadora";
        };
    }

    public static String fromPath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        for (String supported : SUPPORTED) {
            String slug = slug(supported);
            if (path.contains("/" + slug) || path.endsWith(slug)) {
                return supported;
            }
        }
        return null;
    }

    public static Map<String, Object> httpContract(String dominio) {
        String canonical = requireSupported(dominio);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("domain", canonical);
        Map<String, Object> success = new LinkedHashMap<>();
        success.put("catalogo", 200);
        success.put("workspace", 200);
        success.put("ajuda", 200);
        success.put("assistente", 200);
        success.put("iaFinanceira", 200);
        success.put("iaFinanceiraExecute", 200);
        success.put("json", 200);
        success.put("pdf", 200);
        success.put("bootstrap", 200);
        success.put("tabelasOficiais", 200);
        success.put("referenciasEconomicas", 200);
        success.put("experiencePreference", 200);
        success.put("liveAjuizamentoAssist", 200);
        map.put("success", Map.copyOf(success));
        map.put("errors", Map.of(
                "dominioInvalido", 422,
                "validationError", 400,
                "constraintViolation", 400,
                "missingParameter", 400,
                "requestBindingError", 400,
                "unsupportedMediaType", 415,
                "methodNotAllowed", 405,
                "rateLimited", 429,
                "businessRule", 422,
                "internalError", 500
        ));
        map.put("pdfHeaders", List.of("Content-Disposition", "X-Request-Id", "X-PJB-Calculation-Domain", "X-PJB-Calculation-File-Name", "X-PJB-Api-Family", "X-PJB-Api-Route-Status", "X-PJB-Api-Operation", "X-PJB-Contract-Version", "X-PJB-Contract-Fingerprint"));
        map.put("liveAssistHeaders", List.of("X-Request-Id", "X-PJB-Api-Family", "X-PJB-Api-Route-Status", "X-PJB-Api-Operation", "X-PJB-Frontend-Ready", "X-PJB-Contract-Version", "X-PJB-Contract-Fingerprint"));
        map.put("observabilityHeaders", List.of("X-Request-Id", "X-PJB-Api-Family", "X-PJB-Api-Route-Status", "X-PJB-Api-Operation", "X-PJB-Calculation-Domain", "X-PJB-Frontend-Ready", "X-PJB-Contract-Version", "X-PJB-Contract-Fingerprint"));
        map.put("aiRoutes", Map.of("financeira", financialAiRoute(canonical)));
        map.put("experiencePreferenceContextFields", List.of("ramoDireito", "classeProcessual", "tipoCausa", "perfilEquipe", "tribunal", "sistemaOrigem"));
        map.put("institutionalPolicyDimensions", List.of("dominio", "ramoDireito", "classeProcessual", "tipoCausa", "perfilEquipe", "tribunal", "sistemaOrigem"));
        map.put("routePolicy", routePolicy(canonical));
        map.put("officialTablesRoute", officialTablesRoute(canonical));
        map.put("problemMediaType", "application/problem+json");
        if ("FEDERAL_PREVIDENCIARIO_CJF".equals(canonical)) {
            map.put("paymentClassifier", List.of("RPV", "PRECATORIO", "CLASSIFICACAO_PARAMETRIZADA"));
        }
        return Map.copyOf(map);
    }

    public static String suggestedDomain(String dominio) {
        String normalized = normalize(dominio);
        if (normalized.contains("TRAB")) {
            return "TRABALHISTA_CLT";
        }
        if (normalized.contains("FAZENDA") || normalized.contains("TRIBUT")) {
            return "FAZENDA_TRIBUTARIO";
        }
        if (normalized.contains("PREVID") || normalized.contains("JEF") || normalized.contains("CJF") || normalized.contains("FEDERAL")) {
            return "FEDERAL_PREVIDENCIARIO_CJF";
        }
        if (normalized.contains("CUSTA") || normalized.contains("DESPESA") || normalized.contains("DEPOSITO")) {
            return "CUSTAS_PROCESSUAIS";
        }
        return null;
    }
}
