package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialExperienceContext;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CalculoJudicialFrontendContractService {

    private static final String VERSION = "v1";
    private static final String FINGERPRINT = "pjb-calculo-front-v1-r56";
    private static final Instant RELEASED_AT = Instant.parse("2026-03-29T12:00:00Z");
    private static final String CACHE_CONTROL = "private, max-age=300, stale-while-revalidate=60";
    private static final String TRANSPORT = "application/problem+json";

    private final CalculoJudicialTabelaOficialService tabelaOficialService;
    private final CalculoJudicialEconomicReferenceService economicReferenceService;

    public CalculoJudicialFrontendContractService(CalculoJudicialTabelaOficialService tabelaOficialService,
                                                  CalculoJudicialEconomicReferenceService economicReferenceService) {
        this.tabelaOficialService = tabelaOficialService;
        this.economicReferenceService = economicReferenceService;
    }

    public String version() {
        return VERSION;
    }

    public String fingerprint() {
        return FINGERPRINT;
    }

    public Instant releasedAt() {
        return RELEASED_AT;
    }

    public String basePath() {
        return CalculoJudicialDomainSupport.basePath();
    }

    public String cacheControl() {
        return CACHE_CONTROL;
    }

    public Map<String, Object> apiCatalog() {
        Map<String, Object> catalog = new LinkedHashMap<>();
        catalog.put("version", version());
        catalog.put("fingerprint", fingerprint());
        catalog.put("releasedAt", releasedAt().toString());
        catalog.put("basePath", basePath());
        catalog.put("catalogo", CalculoJudicialDomainSupport.catalogRoute());
        catalog.put("tabelasOficiais", CalculoJudicialDomainSupport.officialTablesRoute());
        catalog.put("referenciasEconomicas", CalculoJudicialDomainSupport.economicReferencesRoute());
        catalog.put("experiencePreference", CalculoJudicialDomainSupport.experiencePreferenceRoute());
        catalog.put("iaFinanceiraBase", CalculoJudicialDomainSupport.financialAiBaseRoute());
        catalog.put("iaFinanceiraLiveAjuizamento", CalculoJudicialDomainSupport.financialAiLiveFilingRoute());
        catalog.put("dominiosSuportados", CalculoJudicialDomainSupport.supportedDomains());
        catalog.put("aliases", Map.of(
                "TRABALHISTA_CLT", CalculoJudicialDomainSupport.aliases("TRABALHISTA_CLT"),
                "FAZENDA_TRIBUTARIO", CalculoJudicialDomainSupport.aliases("FAZENDA_TRIBUTARIO"),
                "CUSTAS_PROCESSUAIS", CalculoJudicialDomainSupport.aliases("CUSTAS_PROCESSUAIS"),
                "FEDERAL_PREVIDENCIARIO_CJF", CalculoJudicialDomainSupport.aliases("FEDERAL_PREVIDENCIARIO_CJF")
        ));
        catalog.put("rotas", Map.of(
                "TRABALHISTA_CLT", CalculoJudicialDomainSupport.apiRoutes("TRABALHISTA_CLT"),
                "FAZENDA_TRIBUTARIO", CalculoJudicialDomainSupport.apiRoutes("FAZENDA_TRIBUTARIO"),
                "CUSTAS_PROCESSUAIS", CalculoJudicialDomainSupport.apiRoutes("CUSTAS_PROCESSUAIS"),
                "FEDERAL_PREVIDENCIARIO_CJF", CalculoJudicialDomainSupport.apiRoutes("FEDERAL_PREVIDENCIARIO_CJF")
        ));
        catalog.put("routePolicies", Map.of(
                "TRABALHISTA_CLT", CalculoJudicialDomainSupport.routePolicy("TRABALHISTA_CLT"),
                "FAZENDA_TRIBUTARIO", CalculoJudicialDomainSupport.routePolicy("FAZENDA_TRIBUTARIO"),
                "CUSTAS_PROCESSUAIS", CalculoJudicialDomainSupport.routePolicy("CUSTAS_PROCESSUAIS"),
                "FEDERAL_PREVIDENCIARIO_CJF", CalculoJudicialDomainSupport.routePolicy("FEDERAL_PREVIDENCIARIO_CJF")
        ));
        catalog.put("officialBenchmarkCoverage", CalculatorHelpMessages.officialBenchmarkSignals());
        catalog.put("agentesIa", aiAgentsCatalog());
        catalog.put("painelIaFinanceira", financialAiPanel());
        catalog.put("experienceModes", experienceModes(null, CalculoJudicialSolicitantePerfil.ADVOGADO));
        catalog.put("experiencePreferenceContextFields", experiencePreferenceContextFields());
        catalog.put("referenciasEconomicasProfile", economicReferenceService.panelSnapshot());
        catalog.put("expansionIdeas", CalculatorHelpMessages.expansionIdeas());
        catalog.put("tabelasOficiaisProfile", ordered(
                "route", CalculoJudicialDomainSupport.officialTablesRoute(),
                "version", tabelaOficialService.catalog(null).version(),
                "fingerprint", tabelaOficialService.catalog(null).fingerprint()
        ));
        catalog.put("cache", cacheDescriptor("catalogo", null, null));
        return Map.copyOf(catalog);
    }

    public Map<String, Object> apiContract(String dominio) {
        String canonical = CalculoJudicialDomainSupport.requireSupported(dominio);
        Map<String, Object> contract = new LinkedHashMap<>(CalculoJudicialDomainSupport.httpContract(canonical));
        contract.put("version", version());
        contract.put("fingerprint", fingerprint());
        contract.put("releasedAt", releasedAt().toString());
        contract.put("basePath", basePath());
        contract.put("dominioCanonico", canonical);
        contract.put("slug", CalculoJudicialDomainSupport.slug(canonical));
        contract.put("workspaceRoute", CalculoJudicialDomainSupport.workspaceRoute(canonical));
        contract.put("ajudaRoute", CalculoJudicialDomainSupport.helpRoute(canonical));
        contract.put("assistenteRoute", CalculoJudicialDomainSupport.assistenteRoute(canonical));
        contract.put("iaFinanceiraRoute", CalculoJudicialDomainSupport.financialAiRoute(canonical));
        contract.put("iaFinanceiraExecuteRoute", CalculoJudicialDomainSupport.financialAiExecuteRoute());
        contract.put("jsonRoute", CalculoJudicialDomainSupport.jsonRoute(canonical));
        contract.put("pdfRoute", CalculoJudicialDomainSupport.pdfRoute(canonical));
        contract.put("catalogRoute", CalculoJudicialDomainSupport.catalogRoute(canonical));
        contract.put("bootstrapRoute", CalculoJudicialDomainSupport.bootstrapRoute(canonical));
        contract.put("officialTablesRoute", CalculoJudicialDomainSupport.officialTablesRoute(canonical));
        contract.put("experiencePreferenceRoute", CalculoJudicialDomainSupport.experiencePreferenceRoute());
        contract.put("experiencePreferenceRouteByDomain", CalculoJudicialDomainSupport.experiencePreferenceRoute(canonical));
        contract.put("economicReferencesRoute", CalculoJudicialDomainSupport.economicReferencesRoute());
        contract.put("liveAjuizamentoAssistRoute", CalculoJudicialDomainSupport.financialAiLiveFilingRoute());
        contract.put("officialTablesProfile", tabelaOficialService.profile(canonical));
        contract.put("aiAgents", aiAgentsCatalog());
        contract.put("routePolicy", CalculoJudicialDomainSupport.routePolicy(canonical));
        contract.put("experiencePreferenceContextFields", experiencePreferenceContextFields());
        contract.put("cache", cacheDescriptor("domain_contract", canonical, null));
        return Map.copyOf(contract);
    }

    public Map<String, Object> frontendBindings(String dominio) {
        String canonical = CalculoJudicialDomainSupport.requireSupported(dominio);
        Map<String, Object> bindings = new LinkedHashMap<>();
        bindings.put("workspace", "calculadoras");
        bindings.put("helpMessages", "metadata.helpMessages");
        bindings.put("progressModel", "metadata.progressModel");
        bindings.put("workflowState", "metadata.workflowState");
        bindings.put("completionExperience", "metadata.completionExperience");
        bindings.put("bootstrapRoute", "metadata.frontendBootstrapRoute");
        bindings.put("catalogRoute", "metadata.frontendCatalogRoute");
        bindings.put("apiContract", "metadata.apiContract");
        bindings.put("iaFinanceiraRoute", "metadata.iaFinanceiraRoute");
        bindings.put("iaFinanceiraExecuteRoute", "metadata.iaFinanceiraExecuteRoute");
        bindings.put("aiAgentCatalog", "metadata.aiAgentCatalog");
        bindings.put("routePolicy", "metadata.routePolicy");
        bindings.put("officialTablesProfile", "metadata.officialTablesProfile");
        bindings.put("officialTablesRoute", "metadata.officialTablesRoute");
        bindings.put("profileCapabilities", "metadata.profileCapabilities");
        bindings.put("contractFingerprint", fingerprint());
        bindings.put("contractVersion", version());
        bindings.put("domain", canonical);
        return Map.copyOf(bindings);
    }

    public Map<String, Object> profileCapabilities(CalculoJudicialSolicitantePerfil perfil) {
        CalculoJudicialSolicitantePerfil effectiveProfile = Objects.requireNonNullElse(perfil, CalculoJudicialSolicitantePerfil.CIDADAO);
        boolean citizenLike = effectiveProfile.citizenLike();
        boolean technicalLike = effectiveProfile.technicalLike();
        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("profile", effectiveProfile.name());
        capabilities.put("tone", citizenLike ? "guiado" : "auditavel");
        capabilities.put("quickStartMode", citizenLike ? "step_by_step" : "audit_first");
        capabilities.put("canUseIaAssistiva", Boolean.TRUE);
        capabilities.put("canUseIaFinanceira", Boolean.TRUE);
        capabilities.put("canUseIaConferenciaRecursal", Boolean.TRUE);
        capabilities.put("manualModeAlwaysAvailable", Boolean.TRUE);
        capabilities.put("aiModeOptional", Boolean.TRUE);
        capabilities.put("canSeeFinancialAiPanel", Boolean.TRUE);
        capabilities.put("canUseLiveFilingSignals", Boolean.TRUE);
        capabilities.put("defaultExperienceMode", defaultExperienceMode(effectiveProfile));
        capabilities.put("modeSelectorMessage", modeSelectorMessage(effectiveProfile));
        capabilities.put("experiencePreferenceRoute", CalculoJudicialDomainSupport.experiencePreferenceRoute());
        capabilities.put("entryExperienceModes", experienceModes(null, effectiveProfile));
        capabilities.put("agenticFinancialRouterMode", "planner_router_normalizer_validator_executor_verifier");
        capabilities.put("financialAiExecutionMode", technicalLike ? "assistir_e_executar" : "guiar_e_executar_com_confirmacao");
        capabilities.put("canGeneratePdf", Boolean.TRUE);
        capabilities.put("canExportJson", Boolean.TRUE);
        capabilities.put("canSeeTechnicalExplanation", technicalLike);
        capabilities.put("canEditSensitiveRates", technicalLike);
        capabilities.put("canUseHonorariosAdvanced", technicalLike);
        capabilities.put("canUseObservacoesTecnicas", technicalLike);
        capabilities.put("visibleSectionsMode", citizenLike ? "essential_first" : "full");
        capabilities.put("domainAccess", Map.of(
                "TRABALHISTA_CLT", Boolean.TRUE,
                "FAZENDA_TRIBUTARIO", Boolean.TRUE,
                "CUSTAS_PROCESSUAIS", Boolean.TRUE,
                "FEDERAL_PREVIDENCIARIO_CJF", Boolean.TRUE
        ));
        capabilities.put("recommendedCollapsedSections", citizenLike
                ? List.of("Atualização", "Penalidades e encargos", "Encargos e honorários")
                : List.of());
        capabilities.put("aiAgents", aiAgentsCatalog());
        capabilities.put("financialAiPanel", financialAiPanel());
        return Map.copyOf(capabilities);
    }

    public Map<String, Object> frontendMeta(String dominio, CalculoJudicialSolicitantePerfil perfil, String scope) {
        String canonical = CalculoJudicialDomainSupport.requireSupported(dominio);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("frontendCatalogRoute", CalculoJudicialDomainSupport.catalogRoute(canonical));
        meta.put("frontendBootstrapRoute", CalculoJudicialDomainSupport.bootstrapRoute(canonical));
        meta.put("officialTablesRoute", CalculoJudicialDomainSupport.officialTablesRoute(canonical));
        meta.put("economicReferencesRoute", CalculoJudicialDomainSupport.economicReferencesRoute());
        meta.put("iaFinanceiraRoute", CalculoJudicialDomainSupport.financialAiRoute(canonical));
        meta.put("iaFinanceiraExecuteRoute", CalculoJudicialDomainSupport.financialAiExecuteRoute());
        meta.put("iaFinanceiraLiveAjuizamentoRoute", CalculoJudicialDomainSupport.financialAiLiveFilingRoute());
        meta.put("aiAgentCatalog", aiAgentsCatalog());
        meta.put("officialTablesProfile", tabelaOficialService.profile(canonical));
        meta.put("frontendBindings", frontendBindings(canonical));
        meta.put("profileCapabilities", profileCapabilities(perfil));
        meta.put("routePolicy", CalculoJudicialDomainSupport.routePolicy(canonical));
        meta.put("experienceModes", experienceModes(canonical, perfil));
        meta.put("defaultExperienceMode", defaultExperienceMode(perfil));
        meta.put("modeSelectorMessage", modeSelectorMessage(perfil));
        meta.put("experiencePreferenceRoute", CalculoJudicialDomainSupport.experiencePreferenceRoute());
        meta.put("experiencePreferenceRouteByDomain", CalculoJudicialDomainSupport.experiencePreferenceRoute(canonical));
        meta.put("experiencePreferenceContextFields", experiencePreferenceContextFields());
        meta.put("apiContract", apiContract(canonical));
        meta.put("officialBenchmarkCoverage", CalculatorHelpMessages.officialBenchmarkSignals());
        meta.put("expansionIdeas", CalculatorHelpMessages.expansionIdeas());
        meta.put("financialAiPanel", financialAiPanel());
        meta.put("economicReferences", economicReferenceService.panelSnapshot());
        meta.put("financialKnowledgeBase", financialKnowledgeBase());
        meta.put("contractVersion", version());
        meta.put("contractFingerprint", fingerprint());
        meta.put("cache", cacheDescriptor(scope, canonical, perfil));
        return Map.copyOf(meta);
    }

    public Map<String, Object> uiCatalog(CalculoJudicialSolicitantePerfil perfil) {
        Map<String, Object> ui = new LinkedHashMap<>();
        ui.put("menuPrincipal", "Calculadora");
        ui.put("layout", "wizard_sidebar_sticky_summary");
        ui.put("perfilTomPadrao", Objects.requireNonNullElse(perfil, CalculoJudicialSolicitantePerfil.CIDADAO).citizenLike() ? "guiado" : "auditavel");
        ui.put("abas", List.of("Visão geral", "Trabalhista CLT", "Fazenda e Tributário", "Custas e Despesas", "Federal/JEF Previdenciário", "Ajuda", "IA assistiva"));
        ui.put("componentes", List.of("cards de entrada", "resumo lateral fixo", "mensagens por seção", "toast", "banner", "card de conclusão", "badge", "action bar"));
        ui.put("aiAgents", aiAgentsCatalog());
        ui.put("experienceModes", experienceModes(null, perfil));
        ui.put("defaultExperienceMode", defaultExperienceMode(perfil));
        ui.put("modeSelectorMessage", modeSelectorMessage(perfil));
        ui.put("experiencePreferenceRoute", CalculoJudicialDomainSupport.experiencePreferenceRoute());
        ui.put("experiencePreferenceContextFields", experiencePreferenceContextFields());
        ui.put("financialAiPanel", financialAiPanel());
        ui.put("economicReferences", economicReferenceService.panelSnapshot());
        ui.put("financialKnowledgeBase", financialKnowledgeBase());
        ui.put("workflowStates", List.of("READY", "PENDING", "BLOCKED"));
        ui.put("frontendRoutes", ordered(
                "workspace", CalculoJudicialDomainSupport.workspaceRoute(),
                "catalogo", CalculoJudicialDomainSupport.catalogRoute(),
                "tabelasOficiais", CalculoJudicialDomainSupport.officialTablesRoute(),
                "iaFinanceiraBase", CalculoJudicialDomainSupport.financialAiBaseRoute(),
                "iaFinanceiraExecute", CalculoJudicialDomainSupport.financialAiExecuteRoute(),
                "iaFinanceiraLiveAjuizamento", CalculoJudicialDomainSupport.financialAiLiveFilingRoute(),
                "experiencePreference", CalculoJudicialDomainSupport.experiencePreferenceRoute(),
                "experiencePreferenceTrabalhista", CalculoJudicialDomainSupport.experiencePreferenceRoute("TRABALHISTA_CLT"),
                "experiencePreferenceFazenda", CalculoJudicialDomainSupport.experiencePreferenceRoute("FAZENDA_TRIBUTARIO"),
                "experiencePreferenceCustas", CalculoJudicialDomainSupport.experiencePreferenceRoute("CUSTAS_PROCESSUAIS"),
                "experiencePreferenceFederalPrevidenciario", CalculoJudicialDomainSupport.experiencePreferenceRoute("FEDERAL_PREVIDENCIARIO_CJF"),
                "referenciasEconomicas", CalculoJudicialDomainSupport.economicReferencesRoute(),
                "iaConferenciaRecursal", CalculoJudicialDomainSupport.recursalAiRoute(),
                "recursalAdmissibilidadeReal", CalculoJudicialDomainSupport.recursalAdmissibilityRoute(),
                "bootstrapTrabalhista", CalculoJudicialDomainSupport.bootstrapRoute("TRABALHISTA_CLT"),
                "bootstrapFazenda", CalculoJudicialDomainSupport.bootstrapRoute("FAZENDA_TRIBUTARIO"),
                "bootstrapCustas", CalculoJudicialDomainSupport.bootstrapRoute("CUSTAS_PROCESSUAIS"),
                "bootstrapFederalPrevidenciario", CalculoJudicialDomainSupport.bootstrapRoute("FEDERAL_PREVIDENCIARIO_CJF"),
                "iaFinanceiraTrabalhista", CalculoJudicialDomainSupport.financialAiRoute("TRABALHISTA_CLT"),
                "iaFinanceiraFazenda", CalculoJudicialDomainSupport.financialAiRoute("FAZENDA_TRIBUTARIO"),
                "iaFinanceiraCustas", CalculoJudicialDomainSupport.financialAiRoute("CUSTAS_PROCESSUAIS"),
                "iaFinanceiraFederalPrevidenciario", CalculoJudicialDomainSupport.financialAiRoute("FEDERAL_PREVIDENCIARIO_CJF")
        ));
        ui.put("routePolicies", Map.of(
                "TRABALHISTA_CLT", CalculoJudicialDomainSupport.routePolicy("TRABALHISTA_CLT"),
                "FAZENDA_TRIBUTARIO", CalculoJudicialDomainSupport.routePolicy("FAZENDA_TRIBUTARIO"),
                "CUSTAS_PROCESSUAIS", CalculoJudicialDomainSupport.routePolicy("CUSTAS_PROCESSUAIS"),
                "FEDERAL_PREVIDENCIARIO_CJF", CalculoJudicialDomainSupport.routePolicy("FEDERAL_PREVIDENCIARIO_CJF")
        ));
        ui.put("preferredApiFamily", "calculos");
        ui.put("profileCapabilities", profileCapabilities(perfil));
        ui.put("contractVersion", version());
        ui.put("contractFingerprint", fingerprint());
        ui.put("officialBenchmarkCoverage", CalculatorHelpMessages.officialBenchmarkSignals());
        ui.put("expansionIdeas", CalculatorHelpMessages.expansionIdeas());
        ui.put("cache", cacheDescriptor("ui_catalog", null, perfil));
        return Map.copyOf(ui);
    }

    public Map<String, Object> errorCatalog() {
        Map<String, Object> errors = new LinkedHashMap<>();
        errors.put("transport", TRANSPORT);
        errors.put("fields", List.of("type", "title", "status", "detail", "instance", "timestamp", "requestId", "fieldErrors", "supportedDomains", "frontendCatalogRoute", "frontendOfficialTablesRoute", "frontendEconomicReferencesRoute", "frontendLiveAjuizamentoAssistRoute", "frontendBootstrapRoute", "domainHint", "contractVersion", "contractFingerprint"));
        errors.put("frontendCatalogRoute", CalculoJudicialDomainSupport.catalogRoute());
        errors.put("frontendOfficialTablesRoute", CalculoJudicialDomainSupport.officialTablesRoute());
        errors.put("frontendEconomicReferencesRoute", CalculoJudicialDomainSupport.economicReferencesRoute());
        errors.put("frontendLiveAjuizamentoAssistRoute", CalculoJudicialDomainSupport.financialAiLiveFilingRoute());
        errors.put("frontendExperiencePreferenceRoute", CalculoJudicialDomainSupport.experiencePreferenceRoute());
        errors.put("experiencePreferenceContextFields", experiencePreferenceContextFields());
        errors.put("frontendBootstrapRoutes", Map.of(
                "TRABALHISTA_CLT", CalculoJudicialDomainSupport.bootstrapRoute("TRABALHISTA_CLT"),
                "FAZENDA_TRIBUTARIO", CalculoJudicialDomainSupport.bootstrapRoute("FAZENDA_TRIBUTARIO"),
                "CUSTAS_PROCESSUAIS", CalculoJudicialDomainSupport.bootstrapRoute("CUSTAS_PROCESSUAIS"),
                "FEDERAL_PREVIDENCIARIO_CJF", CalculoJudicialDomainSupport.bootstrapRoute("FEDERAL_PREVIDENCIARIO_CJF")
        ));
        errors.put("routePolicies", Map.of(
                "TRABALHISTA_CLT", CalculoJudicialDomainSupport.routePolicy("TRABALHISTA_CLT"),
                "FAZENDA_TRIBUTARIO", CalculoJudicialDomainSupport.routePolicy("FAZENDA_TRIBUTARIO"),
                "CUSTAS_PROCESSUAIS", CalculoJudicialDomainSupport.routePolicy("CUSTAS_PROCESSUAIS"),
                "FEDERAL_PREVIDENCIARIO_CJF", CalculoJudicialDomainSupport.routePolicy("FEDERAL_PREVIDENCIARIO_CJF")
        ));
        errors.put("supportedDomains", CalculoJudicialDomainSupport.supportedDomains());
        errors.put("contractVersion", version());
        errors.put("contractFingerprint", fingerprint());
        errors.put("cache", cacheDescriptor("error_catalog", null, null));
        return Map.copyOf(errors);
    }


    public Map<String, Object> aiAgentsCatalog() {
        Map<String, Object> agents = new LinkedHashMap<>();
        agents.put("financeira", ordered(
                "agentCode", "IA_FINANCEIRA_PJB",
                "title", "IA financeira do PJB",
                "mission", "Receber um pedido, roteá-lo para o domínio correto, preencher apenas o que for determinístico, chamar a calculadora real e devolver memória auditável sem inventar rubrica, sempre com alternativa manual disponível.",
                "entryBaseRoute", CalculoJudicialDomainSupport.financialAiBaseRoute(),
                "entryExecuteRoute", CalculoJudicialDomainSupport.financialAiExecuteRoute(),
                "manualAlternativeRoute", CalculoJudicialDomainSupport.workspaceRoute(),
                "supportedDomains", CalculoJudicialDomainSupport.supportedDomains(),
                "routes", Map.of(
                        "execute", CalculoJudicialDomainSupport.financialAiExecuteRoute(),
                        "TRABALHISTA_CLT", CalculoJudicialDomainSupport.financialAiRoute("TRABALHISTA_CLT"),
                        "FAZENDA_TRIBUTARIO", CalculoJudicialDomainSupport.financialAiRoute("FAZENDA_TRIBUTARIO"),
                        "CUSTAS_PROCESSUAIS", CalculoJudicialDomainSupport.financialAiRoute("CUSTAS_PROCESSUAIS"),
                        "FEDERAL_PREVIDENCIARIO_CJF", CalculoJudicialDomainSupport.financialAiRoute("FEDERAL_PREVIDENCIARIO_CJF")
                ),
                "preferredRoute", CalculoJudicialDomainSupport.financialAiExecuteRoute(),
                "liveAjuizamentoRoute", CalculoJudicialDomainSupport.financialAiLiveFilingRoute(),
                "alwaysVisibleOnPanel", Boolean.TRUE,
                "economicReferencesRoute", CalculoJudicialDomainSupport.economicReferencesRoute(),
                "executionModel", "planner_router_normalizer_validator_executor_verifier",
                "schemaDiscipline", "strict_payload_to_typed_request",
                "verificationMode", "post_execution_consistency_gate",
                "methods2026", CalculatorHelpMessages.financialIa2026Methods(),
                "guardrails", CalculatorHelpMessages.iaGuardrails(),
                "capabilities", CalculatorHelpMessages.safeAutomationCapabilities()
        ));
        agents.put("conferenciaRecursal", ordered(
                "agentCode", "IA_CONFERENCIA_RECURSAL_PJB",
                "title", "IA de conferência recursal do PJB",
                "mission", "Conferir admissibilidade, preparo, tempestividade e coerência operacional do recurso usando o núcleo recursal real do PJB.",
                "entryRoute", CalculoJudicialDomainSupport.recursalAiRoute(),
                "admissibilityRoute", CalculoJudicialDomainSupport.recursalAdmissibilityRoute(),
                "executionModel", "planner_checker_real_admissibility_verifier",
                "schemaDiscipline", "strict_recursal_payload",
                "verificationMode", "admissibility_real_plus_risk_gate",
                "methods2026", CalculatorHelpMessages.recursalIaMessages(),
                "guardrails", CalculatorHelpMessages.iaGuardrails(),
                "supportedDomains", List.of("RECURSAL_CONFERENCIA"),
                "capabilities", List.of("conferir tempestividade", "conferir preparo", "conferir preclusão", "conferir risco operacional", "chamar admissibilidade real")
        ));
        return Map.copyOf(agents);
    }

    public Map<String, Object> cacheDescriptor(String scope, String dominio, CalculoJudicialSolicitantePerfil perfil) {
        Map<String, Object> cache = new LinkedHashMap<>();
        cache.put("scope", scope == null ? "unknown" : scope);
        cache.put("visibility", "private");
        cache.put("cacheControl", cacheControl());
        cache.put("vary", List.of("Authorization", "X-Equipe-ID"));
        cache.put("etag", eTag(scope, dominio, perfil));
        cache.put("version", version());
        cache.put("fingerprint", fingerprint());
        return Map.copyOf(cache);
    }

    public String eTag(String scope, String dominio, CalculoJudicialSolicitantePerfil perfil) {
        String canonical = dominio == null || dominio.isBlank() ? "GLOBAL" : CalculoJudicialDomainSupport.normalize(dominio);
        String profile = perfil == null ? "DEFAULT" : perfil.name();
        return '"' + sha256(scope + '|' + canonical + '|' + profile + '|' + fingerprint()) + '"';
    }

    public Map<String, String> frontendResponseHeaders(String scope, String dominio, CalculoJudicialSolicitantePerfil perfil) {
        String canonical = dominio == null || dominio.isBlank() ? "GLOBAL" : CalculoJudicialDomainSupport.normalize(dominio);
        return Map.of(
                "Cache-Control", cacheControl(),
                "ETag", eTag(scope, canonical, perfil),
                "Vary", "Authorization, X-Equipe-ID",
                "X-PJB-Contract-Version", version(),
                "X-PJB-Contract-Fingerprint", fingerprint(),
                "X-PJB-Frontend-Ready", "true",
                "X-PJB-Api-Family", "calculos",
                "X-PJB-Api-Route-Status", "canonical"
        );
    }


    public Map<String, Object> financialAiPanel() {
        Map<String, Object> panel = new LinkedHashMap<>();
        panel.put("panelCode", "PAINEL_IA_FINANCEIRA");
        panel.put("title", "IA financeira do PJB");
        panel.put("alwaysVisible", Boolean.TRUE);
        panel.put("entryRoute", CalculoJudicialDomainSupport.financialAiExecuteRoute());
        panel.put("manualWorkspaceRoute", CalculoJudicialDomainSupport.workspaceRoute());
        panel.put("manualModeAlwaysAvailable", Boolean.TRUE);
        panel.put("aiModeOptional", Boolean.TRUE);
        panel.put("modeSelector", List.of("manual_tradicional", "assistido_com_ia"));
        panel.put("experienceModes", experienceModes(null, CalculoJudicialSolicitantePerfil.ADVOGADO));
        panel.put("liveAjuizamentoRoute", CalculoJudicialDomainSupport.financialAiLiveFilingRoute());
        panel.put("economicReferencesRoute", CalculoJudicialDomainSupport.economicReferencesRoute());
        panel.put("supportedDomains", CalculoJudicialDomainSupport.supportedDomains());
        panel.put("experiencePreferenceRoute", CalculoJudicialDomainSupport.experiencePreferenceRoute());
        panel.put("experiencePreferenceContextFields", experiencePreferenceContextFields());
        panel.put("quickActions", List.of("abrir versão manual", "executar cálculo", "sinalizar ajuizamento", "abrir tabelas oficiais", "ver referências econômicas"));
        panel.put("manualAlternativeDescription", "A advocacia pode permanecer no fluxo tradicional sem depender da IA financeira para preencher ou executar cálculos.");
        panel.put("economicReferences", economicReferenceService.panelSnapshot());
        panel.put("knowledgeBase", financialKnowledgeBase());
        return Map.copyOf(panel);
    }

    public Map<String, Object> financialKnowledgeBase() {
        Map<String, Object> knowledge = new LinkedHashMap<>();
        knowledge.put("coveredDomains", CalculoJudicialDomainSupport.supportedDomains());
        knowledge.put("coveredCalculationFamilies", List.of(
                "saldo_de_salario",
                "13o_proporcional",
                "ferias_e_terco",
                "aviso_previo",
                "fgts_e_multa",
                "horas_extras_e_dsr",
                "adicionais_trabalhistas",
                "mora_tributaria",
                "selic_e_acrescimos_legais",
                "custas_preparo_despesas",
                "atrasados_previdenciarios",
                "abono_anual",
                "classificacao_rpv_precatorio",
                "honorarios",
                "multas",
                "valor_da_causa"
        ));
        knowledge.put("officialSignals", CalculatorHelpMessages.officialBenchmarkSignals());
        knowledge.put("economicReferences", economicReferenceService.panelSnapshot());
        knowledge.put("preferenceScopes", List.of("GLOBAL", "DOMAIN", "TEAM", "TEAM_POLICY", "TEAM_CONTEXT", "TEAM_POLICY_CONTEXT", "USER_CONTEXT", "TRIBUNAL_CONTEXT", "SYSTEM_CONTEXT"));
        knowledge.put("livePetitioningSupport", List.of("valor_da_causa", "honorarios", "multas", "dominio_sugerido", "triagem_de_calculo"));
        knowledge.put("preferencePersistence", List.of("usuario_global", "usuario_por_dominio", "usuario_equipe_ativa", "politica_institucional_equipe", "politica_contextual_por_tipo_de_causa", "fallback_por_perfil"));
        return Map.copyOf(knowledge);
    }

    public List<Map<String, Object>> experienceModes(String dominio, CalculoJudicialSolicitantePerfil perfil) {
        CalculoJudicialSolicitantePerfil effectiveProfile = Objects.requireNonNullElse(perfil, CalculoJudicialSolicitantePerfil.CIDADAO);
        String canonical = dominio == null || dominio.isBlank() ? null : CalculoJudicialDomainSupport.requireSupported(dominio);
        Map<String, Object> manual = ordered(
                "code", "manual_tradicional",
                "title", "Versão manual",
                "description", "Fluxo tradicional, sem IA obrigatória, com preenchimento direto, assistente contextual opcional, JSON e PDF.",
                "defaultForProfile", !effectiveProfile.citizenLike(),
                "entryRoute", canonical == null ? CalculoJudicialDomainSupport.workspaceRoute() : CalculoJudicialDomainSupport.workspaceRoute(canonical),
                "assistenteRoute", canonical == null ? null : CalculoJudicialDomainSupport.assistenteRoute(canonical),
                "jsonRoute", canonical == null ? null : CalculoJudicialDomainSupport.jsonRoute(canonical),
                "pdfRoute", canonical == null ? null : CalculoJudicialDomainSupport.pdfRoute(canonical),
                "iaRequired", Boolean.FALSE,
                "recommendedFor", List.of("ADVOGADO", "PROCURADORIA", "CONTADOR_JUDICIAL", "TECNICO_INSTITUCIONAL", "MAGISTRATURA")
        );
        Map<String, Object> assisted = ordered(
                "code", "assistido_com_ia",
                "title", "Versão com IA",
                "description", "Fluxo guiado com IA financeira opcional, autopreenchimento prudencial e execução sobre a calculadora real do PJB.",
                "defaultForProfile", effectiveProfile.citizenLike(),
                "entryRoute", CalculoJudicialDomainSupport.financialAiExecuteRoute(),
                "domainRoute", canonical == null ? null : CalculoJudicialDomainSupport.financialAiRoute(canonical),
                "liveAjuizamentoRoute", CalculoJudicialDomainSupport.financialAiLiveFilingRoute(),
                "iaRequired", Boolean.TRUE,
                "recommendedFor", List.of("CIDADAO", "ADVOGADO", "DEFENSOR_PUBLICO", "PROCURADORIA")
        );
        return List.of(manual, assisted);
    }

    public String defaultExperienceMode(CalculoJudicialSolicitantePerfil perfil) {
        CalculoJudicialSolicitantePerfil effectiveProfile = Objects.requireNonNullElse(perfil, CalculoJudicialSolicitantePerfil.CIDADAO);
        return effectiveProfile.citizenLike() ? "assistido_com_ia" : "manual_tradicional";
    }


    public List<Map<String, Object>> experiencePreferenceContextFields() {
        return List.of(
                ordered("code", "ramoDireito", "label", "Ramo do direito", "type", "string"),
                ordered("code", "classeProcessual", "label", "Classe processual", "type", "string"),
                ordered("code", "tipoCausa", "label", "Tipo de causa", "type", "string"),
                ordered("code", "perfilEquipe", "label", "Perfil da equipe", "type", "string"),
                ordered("code", "tribunal", "label", "Tribunal", "type", "string"),
                ordered("code", "sistemaOrigem", "label", "Sistema de origem", "type", "string")
        );
    }

    public String modeSelectorMessage(CalculoJudicialSolicitantePerfil perfil) {
        CalculoJudicialSolicitantePerfil effectiveProfile = Objects.requireNonNullElse(perfil, CalculoJudicialSolicitantePerfil.CIDADAO);
        return effectiveProfile.citizenLike()
                ? "Você pode usar a versão com IA ou abrir a versão manual tradicional a qualquer momento."
                : "O modo manual tradicional continua disponível como padrão, e a IA financeira permanece opcional para apoio pontual.";
    }

    private Map<String, Object> ordered(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        map.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Map.copyOf(map);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("sha_256_not_available", ex);
        }
    }
}
