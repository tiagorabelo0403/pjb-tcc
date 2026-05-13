package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialExperienceContext;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialWorkspaceCardResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialWorkspaceResponse;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CalculoJudicialWorkspaceService {

    private static final List<String> ABAS = List.of("Visão geral", "Trabalhista CLT", "Fazenda e Tributário", "Custas e Despesas", "Federal/JEF Previdenciário", "Ajuda", "IA assistiva");
    private static final List<String> PERFIS = List.of("CIDADAO", "ADVOGADO", "MAGISTRATURA", "CONTADOR_JUDICIAL", "PROCURADORIA", "TECNICO_INSTITUCIONAL");

    private final CalculoJudicialProfileResolverService profileResolverService;
    private final CalculoJudicialFrontendContractService frontendContractService;
    private final CalculoJudicialExperiencePreferenceService experiencePreferenceService;

    public CalculoJudicialWorkspaceService(CalculoJudicialProfileResolverService profileResolverService,
                                           CalculoJudicialFrontendContractService frontendContractService,
                                           CalculoJudicialExperiencePreferenceService experiencePreferenceService) {
        this.profileResolverService = Objects.requireNonNull(profileResolverService);
        this.frontendContractService = Objects.requireNonNull(frontendContractService);
        this.experiencePreferenceService = Objects.requireNonNull(experiencePreferenceService);
    }

    public CalculoJudicialWorkspaceResponse workspace(Authentication authentication, CalculoJudicialSolicitantePerfil requestedProfile) {
        return workspace(authentication, requestedProfile, null, null);
    }

    public CalculoJudicialWorkspaceResponse workspace(Authentication authentication, CalculoJudicialSolicitantePerfil requestedProfile, String dominio) {
        return workspace(authentication, requestedProfile, dominio, null);
    }

    public CalculoJudicialWorkspaceResponse workspace(Authentication authentication, CalculoJudicialSolicitantePerfil requestedProfile, String dominio, CalculoJudicialExperienceContext context) {
        CalculoJudicialSolicitantePerfil perfil = profileResolverService.resolve(authentication, requestedProfile);
        String dominioCanonico = canonicalOrNull(dominio);
        var experiencePreference = experiencePreferenceService.resolve(authentication, perfil, dominioCanonico, context);
        var resolvedByDomain = experiencePreferenceService.resolvedModesByDomain(authentication, perfil, context);
        Map<String, Object> design = new LinkedHashMap<>();
        design.put("entradaDireta", Boolean.TRUE);
        design.put("menuPrincipal", "Calculadora");
        design.put("layout", "wizard_sidebar_sticky_summary");
        design.put("dominioSolicitado", dominioCanonico == null ? "" : dominioCanonico);
        design.put("navegacao", List.of("cards de entrada", "breadcrumb simples", "validação inline", "painel fixo de resumo", "ação rápida JSON/PDF"));
        design.put("comportamentoAoAbrir", List.of(
                "abrir diretamente a lista de calculadoras sem submenu oculto",
                "mostrar mensagem inicial curta e botão de começar agora",
                "preservar a aba do último domínio usado quando o contexto permitir"
        ));
        design.put("painelLateral", List.of("mensagens detalhadas", "ajuda contextual", "assistência IA", "resumo financeiro ao vivo"));
        design.put("componentesVivos", List.of("toast de conclusão", "banner superior", "card de status", "badge IA assistida", "barra de ações rápidas"));
        design.put("workspaceSignals", CalculatorHelpMessages.smartWorkspaceSignals(perfil));
        design.put("journeys", Map.of(
                "trabalhista", CalculatorHelpMessages.quickStartJourney("TRABALHISTA_CLT", perfil),
                "fazenda", CalculatorHelpMessages.quickStartJourney("FAZENDA_TRIBUTARIO", perfil),
                "custas", CalculatorHelpMessages.quickStartJourney("CUSTAS_PROCESSUAIS", perfil),
                "federalPrevidenciario", CalculatorHelpMessages.quickStartJourney("FEDERAL_PREVIDENCIARIO_CJF", perfil)
        ));
        design.put("apiCatalog", frontendContractService.apiCatalog());
        design.put("aiAgents", frontendContractService.aiAgentsCatalog());
        design.put("financialAiPanel", frontendContractService.financialAiPanel());
        design.put("financialKnowledgeBase", frontendContractService.financialKnowledgeBase());
        design.put("economicReferences", frontendContractService.financialAiPanel().get("economicReferences"));
        design.put("officialBenchmarkCoverage", CalculatorHelpMessages.officialBenchmarkSignals());
        design.put("expansionIdeas", CalculatorHelpMessages.expansionIdeas());
        design.put("frontendCatalogRoute", CalculoJudicialDomainSupport.catalogRoute());
        design.put("contractVersion", frontendContractService.version());
        design.put("contractFingerprint", frontendContractService.fingerprint());
        design.put("profileCapabilities", frontendContractService.profileCapabilities(perfil));
        design.put("experienceModes", frontendContractService.experienceModes(dominioCanonico, perfil));
        design.put("defaultExperienceMode", frontendContractService.defaultExperienceMode(perfil));
        design.put("resolvedExperiencePreference", experiencePreference);
        design.put("resolvedExperiencePreferencesByDomain", resolvedByDomain);
        design.put("resolvedExperienceMode", experiencePreference.resolvedExperienceMode());
        design.put("experiencePreferenceContext", experiencePreference.policyContext());
        design.put("financialIaMessages", CalculatorHelpMessages.financialIaMessages());
        design.put("cache", frontendContractService.cacheDescriptor("workspace", dominioCanonico, perfil));
        design.put("frontendBootstrapRoutes", Map.of(
                "TRABALHISTA_CLT", CalculoJudicialDomainSupport.bootstrapRoute("TRABALHISTA_CLT"),
                "FAZENDA_TRIBUTARIO", CalculoJudicialDomainSupport.bootstrapRoute("FAZENDA_TRIBUTARIO"),
                "CUSTAS_PROCESSUAIS", CalculoJudicialDomainSupport.bootstrapRoute("CUSTAS_PROCESSUAIS"),
                "FEDERAL_PREVIDENCIARIO_CJF", CalculoJudicialDomainSupport.bootstrapRoute("FEDERAL_PREVIDENCIARIO_CJF")
        ));
        design.put("frontendDomainCatalogRoutes", Map.of(
                "TRABALHISTA_CLT", CalculoJudicialDomainSupport.catalogRoute("TRABALHISTA_CLT"),
                "FAZENDA_TRIBUTARIO", CalculoJudicialDomainSupport.catalogRoute("FAZENDA_TRIBUTARIO"),
                "CUSTAS_PROCESSUAIS", CalculoJudicialDomainSupport.catalogRoute("CUSTAS_PROCESSUAIS"),
                "FEDERAL_PREVIDENCIARIO_CJF", CalculoJudicialDomainSupport.catalogRoute("FEDERAL_PREVIDENCIARIO_CJF")
        ));
        design.put("liveAjuizamentoAssistRoute", CalculoJudicialDomainSupport.financialAiLiveFilingRoute());
        design.put("economicReferencesRoute", CalculoJudicialDomainSupport.economicReferencesRoute());
        design.put("entryModesWorkspace", frontendContractService.experienceModes(dominioCanonico, perfil));
        design.put("entryModeDefaultWorkspace", frontendContractService.defaultExperienceMode(perfil));
        design.put("experiencePreferenceRoute", CalculoJudicialDomainSupport.experiencePreferenceRoute());
        design.put("experiencePreferenceContextFields", frontendContractService.experiencePreferenceContextFields());
        design.put("experiencePreferenceRoutesByDomain", Map.of(
                "TRABALHISTA_CLT", CalculoJudicialDomainSupport.experiencePreferenceRoute("TRABALHISTA_CLT"),
                "FAZENDA_TRIBUTARIO", CalculoJudicialDomainSupport.experiencePreferenceRoute("FAZENDA_TRIBUTARIO"),
                "CUSTAS_PROCESSUAIS", CalculoJudicialDomainSupport.experiencePreferenceRoute("CUSTAS_PROCESSUAIS"),
                "FEDERAL_PREVIDENCIARIO_CJF", CalculoJudicialDomainSupport.experiencePreferenceRoute("FEDERAL_PREVIDENCIARIO_CJF")
        ));
        design.put("financialAiRoutes", Map.of(
                "TRABALHISTA_CLT", CalculoJudicialDomainSupport.financialAiExecuteRoute(),
                "FAZENDA_TRIBUTARIO", CalculoJudicialDomainSupport.financialAiExecuteRoute(),
                "CUSTAS_PROCESSUAIS", CalculoJudicialDomainSupport.financialAiExecuteRoute(),
                "FEDERAL_PREVIDENCIARIO_CJF", CalculoJudicialDomainSupport.financialAiExecuteRoute()
        ));
        design.put("financialAiPresets", Map.of(
                "TRABALHISTA_CLT", Map.of("dominio", "TRABALHISTA_CLT"),
                "FAZENDA_TRIBUTARIO", Map.of("dominio", "FAZENDA_TRIBUTARIO"),
                "CUSTAS_PROCESSUAIS", Map.of("dominio", "CUSTAS_PROCESSUAIS"),
                "FEDERAL_PREVIDENCIARIO_CJF", Map.of("dominio", "FEDERAL_PREVIDENCIARIO_CJF")
        ));
        String trabalhistaMode = resolvedModeFromMap(resolvedByDomain, "TRABALHISTA_CLT", frontendContractService.defaultExperienceMode(perfil));
        String fazendaMode = resolvedModeFromMap(resolvedByDomain, "FAZENDA_TRIBUTARIO", frontendContractService.defaultExperienceMode(perfil));
        String custasMode = resolvedModeFromMap(resolvedByDomain, "CUSTAS_PROCESSUAIS", frontendContractService.defaultExperienceMode(perfil));
        String federalMode = resolvedModeFromMap(resolvedByDomain, "FEDERAL_PREVIDENCIARIO_CJF", frontendContractService.defaultExperienceMode(perfil));
        List<CalculoJudicialWorkspaceCardResponse> cards = List.of(cardTrabalhista(perfil, trabalhistaMode), cardFazenda(perfil, fazendaMode), cardCustas(perfil, custasMode), cardFederalPrevidenciario(perfil, federalMode));
        List<CalculoJudicialWorkspaceCardResponse> filteredCards = filterByDomain(cards, dominioCanonico);
        String abaPadrao = filteredCards.isEmpty()
                ? "Calculadora"
                : CalculoJudicialDomainSupport.aba(filteredCards.get(0).codigo());
        return new CalculoJudicialWorkspaceResponse(
                abaPadrao,
                "Central de cálculo judicial do PJB",
                "Acesso direto às calculadoras com navegação guiada, mensagens detalhadas e assistência segura para todos os perfis.",
                perfil,
                ABAS,
                List.of(CalculatorHelpMessages.globalIntro()),
                filteredCards,
                CalculatorHelpMessages.dailyBehavior(),
                CalculatorHelpMessages.iaGuardrails(),
                Map.copyOf(design),
                frontendContractService.releasedAt()
        );
    }

    public CalculoJudicialWorkspaceCardResponse workspaceCard(Authentication authentication,
                                                              CalculoJudicialSolicitantePerfil requestedProfile,
                                                              String dominio) {
        return workspaceCard(authentication, requestedProfile, dominio, null);
    }

    public CalculoJudicialWorkspaceCardResponse workspaceCard(Authentication authentication,
                                                              CalculoJudicialSolicitantePerfil requestedProfile,
                                                              String dominio,
                                                              CalculoJudicialExperienceContext context) {
        CalculoJudicialSolicitantePerfil perfil = profileResolverService.resolve(authentication, requestedProfile);
        String dominioCanonico = CalculoJudicialDomainSupport.requireSupported(dominio);
        String resolvedMode = experiencePreferenceService.resolve(authentication, perfil, dominioCanonico, context).resolvedExperienceMode();
        return switch (dominioCanonico) {
            case "TRABALHISTA_CLT" -> cardTrabalhista(perfil, resolvedMode);
            case "FAZENDA_TRIBUTARIO" -> cardFazenda(perfil, resolvedMode);
            case "CUSTAS_PROCESSUAIS" -> cardCustas(perfil, resolvedMode);
            case "FEDERAL_PREVIDENCIARIO_CJF" -> cardFederalPrevidenciario(perfil, resolvedMode);
            default -> throw new IllegalArgumentException("calculo_judicial_dominio_invalido: " + dominio);
        };
    }

    private List<CalculoJudicialWorkspaceCardResponse> filterByDomain(List<CalculoJudicialWorkspaceCardResponse> cards, String dominio) {
        if (dominio == null || dominio.isBlank()) {
            return cards;
        }
        return cards.stream().filter(card -> CalculoJudicialDomainSupport.matches(dominio, card.codigo(), card.aba())).toList();
    }

    private String resolvedModeFromMap(Map<String, Object> resolvedByDomain, String domainCode, String fallback) {
        if (resolvedByDomain == null || resolvedByDomain.isEmpty()) {
            return fallback;
        }
        Object raw = resolvedByDomain.get(domainCode);
        if (raw instanceof com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialExperiencePreferenceResponse response) {
            return response.resolvedExperienceMode();
        }
        return fallback;
    }

    private String canonicalOrNull(String dominio) {
        if (dominio == null || dominio.isBlank()) {
            return null;
        }
        return CalculoJudicialDomainSupport.requireSupported(dominio);
    }

    private CalculoJudicialWorkspaceCardResponse cardTrabalhista(CalculoJudicialSolicitantePerfil perfil, String resolvedMode) {
        Map<String, Object> design = new LinkedHashMap<>();
        design.put("ctaPrimario", "manual_tradicional".equals(resolvedMode) ? "Abrir versão manual" : "Executar com IA financeira");
        design.put("ctaSecundario", "manual_tradicional".equals(resolvedMode) ? "Executar com IA financeira" : "Abrir versão manual");
        design.put("ctaTerciario", "Executar com IA financeira");
        design.put("tom", perfil.citizenLike() ? "guiado" : "técnico auditável");
        design.put("visaoPadrao", List.of("dados iniciais", "jornada e verbas", "reflexos e FGTS", "atualização", "penalidades e encargos"));
        design.put("liveExperience", CalculatorHelpMessages.liveComponentDesign("TRABALHISTA_CLT", perfil));
        design.putAll(frontendContractService.frontendMeta("TRABALHISTA_CLT", perfil, "workspace_card"));
        design.put("frontendReady", Boolean.TRUE);
        design.put("financialIaRoute", CalculoJudicialDomainSupport.financialAiExecuteRoute());
        design.put("financialIaPresetDomain", "TRABALHISTA_CLT");
        design.put("resolvedExperienceMode", resolvedMode);
        design.put("modeSelectorMessage", frontendContractService.modeSelectorMessage(perfil));
        design.put("experiencePreferenceRoute", CalculoJudicialDomainSupport.experiencePreferenceRoute());
        design.put("experiencePreferenceContextFields", frontendContractService.experiencePreferenceContextFields());
        design.put("experiencePreferenceRoutesByDomain", Map.of(
                "TRABALHISTA_CLT", CalculoJudicialDomainSupport.experiencePreferenceRoute("TRABALHISTA_CLT"),
                "FAZENDA_TRIBUTARIO", CalculoJudicialDomainSupport.experiencePreferenceRoute("FAZENDA_TRIBUTARIO"),
                "CUSTAS_PROCESSUAIS", CalculoJudicialDomainSupport.experiencePreferenceRoute("CUSTAS_PROCESSUAIS"),
                "FEDERAL_PREVIDENCIARIO_CJF", CalculoJudicialDomainSupport.experiencePreferenceRoute("FEDERAL_PREVIDENCIARIO_CJF")
        ));
        design.put("financialAiPanelVisible", Boolean.TRUE);
        design.put("liveAjuizamentoAssistRoute", CalculoJudicialDomainSupport.financialAiLiveFilingRoute());
        design.put("economicReferencesRoute", CalculoJudicialDomainSupport.economicReferencesRoute());
        design.put("experienceModes", frontendContractService.experienceModes("TRABALHISTA_CLT", perfil));
        design.put("defaultExperienceMode", frontendContractService.defaultExperienceMode(perfil));
        design.put("manualEntry", manualEntry("TRABALHISTA_CLT"));
        design.put("iaEntry", iaEntry("TRABALHISTA_CLT"));
        return new CalculoJudicialWorkspaceCardResponse(
                "TRABALHISTA_CLT",
                "Calculadora trabalhista CLT",
                "Entrada direta para verbas rescisórias, reflexos, FGTS, multas e memória técnica em JSON ou PDF.",
                CalculoJudicialDomainSupport.aba("TRABALHISTA_CLT"),
                PERFIS,
                CalculatorHelpMessages.trabalhistaMessages(),
                List.of("Dados iniciais", "Jornada e verbas", "Reflexos e FGTS", "Atualização", "Penalidades e encargos", "Observações"),
                CalculatorHelpMessages.safeAutomationCapabilities(),
                CalculoJudicialDomainSupport.apiRoutes("TRABALHISTA_CLT"),
                Map.copyOf(design)
        );
    }

    private CalculoJudicialWorkspaceCardResponse cardFazenda(CalculoJudicialSolicitantePerfil perfil, String resolvedMode) {
        Map<String, Object> design = new LinkedHashMap<>();
        design.put("ctaPrimario", "manual_tradicional".equals(resolvedMode) ? "Abrir versão manual" : "Executar com IA financeira");
        design.put("ctaSecundario", "manual_tradicional".equals(resolvedMode) ? "Executar com IA financeira" : "Abrir versão manual");
        design.put("ctaTerciario", "Executar com IA financeira");
        design.put("tom", perfil.citizenLike() ? "guiado" : "contencioso auditável");
        design.put("visaoPadrao", List.of("dados do processo", "correção monetária", "juros moratórios", "multas e descontos", "encargos e honorários"));
        design.put("liveExperience", CalculatorHelpMessages.liveComponentDesign("FAZENDA_TRIBUTARIO", perfil));
        design.putAll(frontendContractService.frontendMeta("FAZENDA_TRIBUTARIO", perfil, "workspace_card"));
        design.put("frontendReady", Boolean.TRUE);
        design.put("financialIaRoute", CalculoJudicialDomainSupport.financialAiExecuteRoute());
        design.put("financialIaPresetDomain", "FAZENDA_TRIBUTARIO");
        design.put("resolvedExperienceMode", resolvedMode);
        design.put("modeSelectorMessage", frontendContractService.modeSelectorMessage(perfil));
        design.put("experiencePreferenceRoute", CalculoJudicialDomainSupport.experiencePreferenceRoute());
        design.put("experiencePreferenceContextFields", frontendContractService.experiencePreferenceContextFields());
        design.put("experiencePreferenceRoutesByDomain", Map.of(
                "TRABALHISTA_CLT", CalculoJudicialDomainSupport.experiencePreferenceRoute("TRABALHISTA_CLT"),
                "FAZENDA_TRIBUTARIO", CalculoJudicialDomainSupport.experiencePreferenceRoute("FAZENDA_TRIBUTARIO"),
                "CUSTAS_PROCESSUAIS", CalculoJudicialDomainSupport.experiencePreferenceRoute("CUSTAS_PROCESSUAIS"),
                "FEDERAL_PREVIDENCIARIO_CJF", CalculoJudicialDomainSupport.experiencePreferenceRoute("FEDERAL_PREVIDENCIARIO_CJF")
        ));
        design.put("financialAiPanelVisible", Boolean.TRUE);
        design.put("liveAjuizamentoAssistRoute", CalculoJudicialDomainSupport.financialAiLiveFilingRoute());
        design.put("economicReferencesRoute", CalculoJudicialDomainSupport.economicReferencesRoute());
        design.put("experienceModes", frontendContractService.experienceModes("FAZENDA_TRIBUTARIO", perfil));
        design.put("defaultExperienceMode", frontendContractService.defaultExperienceMode(perfil));
        design.put("manualEntry", manualEntry("FAZENDA_TRIBUTARIO"));
        design.put("iaEntry", iaEntry("FAZENDA_TRIBUTARIO"));
        return new CalculoJudicialWorkspaceCardResponse(
                "FAZENDA_TRIBUTARIO",
                "Calculadora fazenda e tributário",
                "Entrada direta para principal, mora, SELIC, descontos, garantias, encargos e memória técnica em JSON ou PDF.",
                CalculoJudicialDomainSupport.aba("FAZENDA_TRIBUTARIO"),
                PERFIS,
                CalculatorHelpMessages.fazendaMessages(),
                List.of("Dados do processo", "Correção monetária", "Juros moratórios", "Multas e descontos", "Encargos e honorários", "Compensações e garantias"),
                CalculatorHelpMessages.safeAutomationCapabilities(),
                CalculoJudicialDomainSupport.apiRoutes("FAZENDA_TRIBUTARIO"),
                Map.copyOf(design)
        );
    }

    private CalculoJudicialWorkspaceCardResponse cardCustas(CalculoJudicialSolicitantePerfil perfil, String resolvedMode) {
        Map<String, Object> design = new LinkedHashMap<>();
        design.put("ctaPrimario", "manual_tradicional".equals(resolvedMode) ? "Abrir versão manual" : "Executar com IA financeira");
        design.put("ctaSecundario", "manual_tradicional".equals(resolvedMode) ? "Executar com IA financeira" : "Abrir versão manual");
        design.put("ctaTerciario", "Executar com IA financeira");
        design.put("tom", perfil.citizenLike() ? "guiado prático" : "auditoria de guias e despesas");
        design.put("visaoPadrao", List.of("dados básicos", "taxa e preparo", "despesas processuais", "atualização", "abatimentos e depósito"));
        design.put("liveExperience", CalculatorHelpMessages.liveComponentDesign("CUSTAS_PROCESSUAIS", perfil));
        design.putAll(frontendContractService.frontendMeta("CUSTAS_PROCESSUAIS", perfil, "workspace_card"));
        design.put("frontendReady", Boolean.TRUE);
        design.put("financialIaRoute", CalculoJudicialDomainSupport.financialAiExecuteRoute());
        design.put("financialIaPresetDomain", "CUSTAS_PROCESSUAIS");
        design.put("resolvedExperienceMode", resolvedMode);
        design.put("modeSelectorMessage", frontendContractService.modeSelectorMessage(perfil));
        design.put("experiencePreferenceRoute", CalculoJudicialDomainSupport.experiencePreferenceRoute());
        design.put("experiencePreferenceContextFields", frontendContractService.experiencePreferenceContextFields());
        design.put("experiencePreferenceRoutesByDomain", Map.of(
                "TRABALHISTA_CLT", CalculoJudicialDomainSupport.experiencePreferenceRoute("TRABALHISTA_CLT"),
                "FAZENDA_TRIBUTARIO", CalculoJudicialDomainSupport.experiencePreferenceRoute("FAZENDA_TRIBUTARIO"),
                "CUSTAS_PROCESSUAIS", CalculoJudicialDomainSupport.experiencePreferenceRoute("CUSTAS_PROCESSUAIS"),
                "FEDERAL_PREVIDENCIARIO_CJF", CalculoJudicialDomainSupport.experiencePreferenceRoute("FEDERAL_PREVIDENCIARIO_CJF")
        ));
        design.put("financialAiPanelVisible", Boolean.TRUE);
        design.put("liveAjuizamentoAssistRoute", CalculoJudicialDomainSupport.financialAiLiveFilingRoute());
        design.put("economicReferencesRoute", CalculoJudicialDomainSupport.economicReferencesRoute());
        design.put("experienceModes", frontendContractService.experienceModes("CUSTAS_PROCESSUAIS", perfil));
        design.put("defaultExperienceMode", frontendContractService.defaultExperienceMode(perfil));
        design.put("manualEntry", manualEntry("CUSTAS_PROCESSUAIS"));
        design.put("iaEntry", iaEntry("CUSTAS_PROCESSUAIS"));
        return new CalculoJudicialWorkspaceCardResponse(
                "CUSTAS_PROCESSUAIS",
                "Calculadora de custas e despesas",
                "Entrada direta para taxa judiciária, preparo, diligências, despesas, depósitos judiciais e PDF técnico da memória de custas.",
                CalculoJudicialDomainSupport.aba("CUSTAS_PROCESSUAIS"),
                PERFIS,
                CalculatorHelpMessages.custasMessages(),
                List.of("Dados básicos", "Taxa e preparo", "Despesas processuais", "Atualização", "Abatimentos e depósito", "Observações"),
                CalculatorHelpMessages.safeAutomationCapabilities(),
                CalculoJudicialDomainSupport.apiRoutes("CUSTAS_PROCESSUAIS"),
                Map.copyOf(design)
        );
    }

    private CalculoJudicialWorkspaceCardResponse cardFederalPrevidenciario(CalculoJudicialSolicitantePerfil perfil, String resolvedMode) {
        Map<String, Object> design = new LinkedHashMap<>();
        design.put("ctaPrimario", "manual_tradicional".equals(resolvedMode) ? "Abrir versão manual" : "Executar com IA financeira");
        design.put("ctaSecundario", "manual_tradicional".equals(resolvedMode) ? "Executar com IA financeira" : "Abrir versão manual");
        design.put("ctaTerciario", "Executar com IA financeira");
        design.put("tom", perfil.citizenLike() ? "guiado previdenciário" : "atrasados e execução auditável");
        design.put("visaoPadrao", List.of("dados do benefício", "marco temporal", "parcelas e abono", "atualização e juros", "abatimentos", "classificação do pagamento"));
        design.put("liveExperience", CalculatorHelpMessages.liveComponentDesign("FEDERAL_PREVIDENCIARIO_CJF", perfil));
        design.putAll(frontendContractService.frontendMeta("FEDERAL_PREVIDENCIARIO_CJF", perfil, "workspace_card"));
        design.put("frontendReady", Boolean.TRUE);
        design.put("financialIaRoute", CalculoJudicialDomainSupport.financialAiExecuteRoute());
        design.put("financialIaPresetDomain", "FEDERAL_PREVIDENCIARIO_CJF");
        design.put("resolvedExperienceMode", resolvedMode);
        design.put("modeSelectorMessage", frontendContractService.modeSelectorMessage(perfil));
        design.put("experiencePreferenceRoute", CalculoJudicialDomainSupport.experiencePreferenceRoute());
        design.put("experiencePreferenceContextFields", frontendContractService.experiencePreferenceContextFields());
        design.put("experiencePreferenceRoutesByDomain", Map.of(
                "TRABALHISTA_CLT", CalculoJudicialDomainSupport.experiencePreferenceRoute("TRABALHISTA_CLT"),
                "FAZENDA_TRIBUTARIO", CalculoJudicialDomainSupport.experiencePreferenceRoute("FAZENDA_TRIBUTARIO"),
                "CUSTAS_PROCESSUAIS", CalculoJudicialDomainSupport.experiencePreferenceRoute("CUSTAS_PROCESSUAIS"),
                "FEDERAL_PREVIDENCIARIO_CJF", CalculoJudicialDomainSupport.experiencePreferenceRoute("FEDERAL_PREVIDENCIARIO_CJF")
        ));
        design.put("financialAiPanelVisible", Boolean.TRUE);
        design.put("liveAjuizamentoAssistRoute", CalculoJudicialDomainSupport.financialAiLiveFilingRoute());
        design.put("economicReferencesRoute", CalculoJudicialDomainSupport.economicReferencesRoute());
        design.put("experienceModes", frontendContractService.experienceModes("FEDERAL_PREVIDENCIARIO_CJF", perfil));
        design.put("defaultExperienceMode", frontendContractService.defaultExperienceMode(perfil));
        design.put("manualEntry", manualEntry("FEDERAL_PREVIDENCIARIO_CJF"));
        design.put("iaEntry", iaEntry("FEDERAL_PREVIDENCIARIO_CJF"));
        return new CalculoJudicialWorkspaceCardResponse(
                "FEDERAL_PREVIDENCIARIO_CJF",
                "Calculadora federal/JEF previdenciária",
                "Entrada direta para atrasados previdenciários federais, abono anual, compensações, RPV/precatório e PDF técnico auditável.",
                CalculoJudicialDomainSupport.aba("FEDERAL_PREVIDENCIARIO_CJF"),
                PERFIS,
                CalculatorHelpMessages.federalPrevidenciarioMessages(),
                List.of("Dados do benefício", "Marco temporal", "Parcelas e abono", "Atualização e juros", "Abatimentos", "Classificação do pagamento", "Observações"),
                CalculatorHelpMessages.safeAutomationCapabilities(),
                CalculoJudicialDomainSupport.apiRoutes("FEDERAL_PREVIDENCIARIO_CJF"),
                Map.copyOf(design)
        );
    }


    private Map<String, Object> manualEntry(String dominio) {
        Map<String, Object> manual = new LinkedHashMap<>();
        manual.put("code", "manual_tradicional");
        manual.put("title", "Versão manual");
        manual.put("workspaceRoute", CalculoJudicialDomainSupport.workspaceRoute(dominio));
        manual.put("assistenteRoute", CalculoJudicialDomainSupport.assistenteRoute(dominio));
        manual.put("jsonRoute", CalculoJudicialDomainSupport.jsonRoute(dominio));
        manual.put("pdfRoute", CalculoJudicialDomainSupport.pdfRoute(dominio));
        manual.put("iaRequired", Boolean.FALSE);
        return Map.copyOf(manual);
    }

    private Map<String, Object> iaEntry(String dominio) {
        Map<String, Object> assisted = new LinkedHashMap<>();
        assisted.put("code", "assistido_com_ia");
        assisted.put("title", "Versão com IA");
        assisted.put("executeRoute", CalculoJudicialDomainSupport.financialAiExecuteRoute());
        assisted.put("domainRoute", CalculoJudicialDomainSupport.financialAiRoute(dominio));
        assisted.put("liveAjuizamentoRoute", CalculoJudicialDomainSupport.financialAiLiveFilingRoute());
        assisted.put("iaRequired", Boolean.TRUE);
        return Map.copyOf(assisted);
    }
}