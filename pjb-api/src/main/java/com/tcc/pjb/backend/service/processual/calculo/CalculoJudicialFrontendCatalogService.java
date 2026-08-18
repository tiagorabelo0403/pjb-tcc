package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialFrontendBootstrapResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialFrontendCatalogResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialExperienceContext;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialFrontendDomainResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CalculoJudicialFrontendCatalogService {

    private final CalculoJudicialProfileResolverService profileResolverService;
    private final CalculoJudicialFrontendContractService frontendContractService;
    private final CalculoJudicialTabelaOficialService tabelaOficialService;
    private final CalculoJudicialExperiencePreferenceService experiencePreferenceService;
    private final SalarioMinimoNacionalService salarioMinimoNacionalService;

    public CalculoJudicialFrontendCatalogService(CalculoJudicialProfileResolverService profileResolverService,
                                                 CalculoJudicialFrontendContractService frontendContractService,
                                                 CalculoJudicialTabelaOficialService tabelaOficialService,
                                                 CalculoJudicialExperiencePreferenceService experiencePreferenceService,
                                                 SalarioMinimoNacionalService salarioMinimoNacionalService) {
        this.profileResolverService = Objects.requireNonNull(profileResolverService);
        this.frontendContractService = Objects.requireNonNull(frontendContractService);
        this.tabelaOficialService = Objects.requireNonNull(tabelaOficialService);
        this.experiencePreferenceService = Objects.requireNonNull(experiencePreferenceService);
        this.salarioMinimoNacionalService = Objects.requireNonNull(salarioMinimoNacionalService);
    }

    public CalculoJudicialFrontendCatalogResponse catalog(Authentication authentication,
                                                          CalculoJudicialSolicitantePerfil requestedProfile,
                                                          String dominio) {
        return catalog(authentication, requestedProfile, dominio, null);
    }

    public CalculoJudicialFrontendCatalogResponse catalog(Authentication authentication,
                                                          CalculoJudicialSolicitantePerfil requestedProfile,
                                                          String dominio,
                                                          CalculoJudicialExperienceContext context) {
        CalculoJudicialSolicitantePerfil perfil = profileResolverService.resolve(authentication, requestedProfile);
        List<CalculoJudicialFrontendDomainResponse> dominios = filtroDominios(dominio).stream()
                .map(codigo -> domainResponse(authentication, codigo, perfil, context))
                .toList();
        Map<String, Object> ui = new LinkedHashMap<>(frontendContractService.uiCatalog(perfil));
        ui.put("resolvedExperiencePreference", experiencePreferenceService.resolve(authentication, perfil, null, context));
        ui.put("resolvedExperiencePreferencesByDomain", experiencePreferenceService.resolvedModesByDomain(authentication, perfil, context));
        ui.put("experiencePreferenceContext", context == null ? Map.of() : orderedContext(context));
        return new CalculoJudicialFrontendCatalogResponse(
                "Calculadora",
                frontendContractService.version(),
                frontendContractService.basePath(),
                perfil,
                CalculoJudicialDomainSupport.supportedDomains(),
                dominios,
                Map.copyOf(ui),
                frontendContractService.errorCatalog(),
                frontendContractService.releasedAt()
        );
    }

    public CalculoJudicialFrontendBootstrapResponse bootstrap(Authentication authentication,
                                                              CalculoJudicialSolicitantePerfil requestedProfile,
                                                              String dominio) {
        return bootstrap(authentication, requestedProfile, dominio, null);
    }

    public CalculoJudicialFrontendBootstrapResponse bootstrap(Authentication authentication,
                                                              CalculoJudicialSolicitantePerfil requestedProfile,
                                                              String dominio,
                                                              CalculoJudicialExperienceContext context) {
        String canonical = CalculoJudicialDomainSupport.requireSupported(dominio);
        CalculoJudicialSolicitantePerfil perfil = profileResolverService.resolve(authentication, requestedProfile);
        Map<String, Object> http = new LinkedHashMap<>(frontendContractService.apiContract(canonical));
        http.put("resolvedExperiencePreference", experiencePreferenceService.resolve(authentication, perfil, canonical, context));
        http.put("resolvedExperiencePreferencesByDomain", experiencePreferenceService.resolvedModesByDomain(authentication, perfil, context));
        http.put("experiencePreferenceContext", context == null ? Map.of() : orderedContext(context));
        return new CalculoJudicialFrontendBootstrapResponse(
                canonical,
                CalculoJudicialDomainSupport.slug(canonical),
                perfil,
                CalculoJudicialDomainSupport.apiRoutes(canonical),
                Map.copyOf(http),
                frontendContractService.aiAgentsCatalog(),
                tabelaOficialService.profile(canonical),
                defaultPayload(canonical, perfil),
                financialAiRequestExample(canonical, perfil),
                requestExample(canonical, perfil),
                responseExample(canonical, perfil),
                errorExample(canonical),
                frontendContractService.releasedAt()
        );
    }

    private List<String> filtroDominios(String dominio) {
        if (dominio == null || dominio.isBlank()) {
            return CalculoJudicialDomainSupport.supportedDomains();
        }
        return List.of(CalculoJudicialDomainSupport.requireSupported(dominio));
    }

    private CalculoJudicialFrontendDomainResponse domainResponse(Authentication authentication, String dominio, CalculoJudicialSolicitantePerfil perfil, CalculoJudicialExperienceContext context) {
        String canonical = CalculoJudicialDomainSupport.requireSupported(dominio);
        return new CalculoJudicialFrontendDomainResponse(
                canonical,
                CalculoJudicialDomainSupport.slug(canonical),
                CalculoJudicialDomainSupport.aba(canonical),
                title(canonical),
                description(canonical),
                CalculoJudicialDomainSupport.apiRoutes(canonical),
                sections(canonical),
                fields(canonical),
                resultContract(canonical),
                uxContract(canonical, perfil),
                domainErrorContract(canonical),
                withResolvedPreference(frontendContractService.apiContract(canonical), experiencePreferenceService.resolve(authentication, perfil, canonical, context), context),
                frontendContractService.aiAgentsCatalog(),
                tabelaOficialService.profile(canonical),
                defaultPayload(canonical, perfil),
                financialAiRequestExample(canonical, perfil),
                requestExample(canonical, perfil),
                responseExample(canonical, perfil),
                errorExample(canonical)
        );
    }

    private Map<String, Object> withResolvedPreference(Map<String, Object> source, Object preference, CalculoJudicialExperienceContext context) {
        Map<String, Object> copy = new LinkedHashMap<>(source);
        copy.put("resolvedExperiencePreference", preference);
        copy.put("experiencePreferenceContext", context == null ? Map.of() : orderedContext(context));
        return Map.copyOf(copy);
    }

    private Map<String, Object> orderedContext(CalculoJudicialExperienceContext context) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (context.ramoDireito() != null && !context.ramoDireito().isBlank()) {
            map.put("ramoDireito", context.ramoDireito());
        }
        if (context.classeProcessual() != null && !context.classeProcessual().isBlank()) {
            map.put("classeProcessual", context.classeProcessual());
        }
        if (context.tipoCausa() != null && !context.tipoCausa().isBlank()) {
            map.put("tipoCausa", context.tipoCausa());
        }
        if (context.perfilEquipe() != null && !context.perfilEquipe().isBlank()) {
            map.put("perfilEquipe", context.perfilEquipe());
        }
        if (context.tribunal() != null && !context.tribunal().isBlank()) {
            map.put("tribunal", context.tribunal());
        }
        if (context.sistemaOrigem() != null && !context.sistemaOrigem().isBlank()) {
            map.put("sistemaOrigem", context.sistemaOrigem());
        }
        return Map.copyOf(map);
    }

    private String title(String dominio) {
        return switch (dominio) {
            case "TRABALHISTA_CLT" -> "Calculadora trabalhista CLT";
            case "FAZENDA_TRIBUTARIO" -> "Calculadora fazenda e tributário";
            case "CUSTAS_PROCESSUAIS" -> "Calculadora de custas e despesas";
            case "FEDERAL_PREVIDENCIARIO_CJF" -> "Calculadora federal/JEF previdenciária";
            default -> "Calculadora judicial";
        };
    }

    private String description(String dominio) {
        return switch (dominio) {
            case "TRABALHISTA_CLT" -> "Formulário guiado para verbas rescisórias, reflexos, FGTS, multas, INSS, IRRF e PDF técnico.";
            case "FAZENDA_TRIBUTARIO" -> "Formulário guiado para principal, SELIC, multa de mora, descontos, garantias, honorários e PDF técnico.";
            case "CUSTAS_PROCESSUAIS" -> "Formulário guiado para taxa judiciária, preparo, despesas, diligências, depósitos judiciais e PDF técnico.";
            case "FEDERAL_PREVIDENCIARIO_CJF" -> "Formulário guiado para atrasados previdenciários federais, abono anual, compensações, RPV/precatório e PDF técnico.";
            default -> "Contrato de frontend da calculadora judicial.";
        };
    }

    private List<Map<String, Object>> sections(String dominio) {
        return switch (dominio) {
            case "TRABALHISTA_CLT" -> List.of(
                    section("dados_iniciais", "Dados iniciais", List.of("tituloCalculo", "numeroProcesso", "reclamanteNome", "reclamadoNome", "salarioBase", "admissao", "demissao", "tipoDispensa")),
                    section("jornada_verbas", "Jornada e verbas", List.of("cargaHorariaMensalBase", "quantidadeHorasExtras50", "quantidadeHorasExtras100", "quantidadeHorasIntervaloIntrajornada", "quantidadeHorasNoturnas", "percentualAdicionalNoturno", "grauInsalubridade", "percentualPericulosidade", "outrasParcelasFixasMensais")),
                    section("reflexos_fgts", "Reflexos e FGTS", List.of("incluirReflexosEmFeriasDecimoTerceiroFgts", "incluirFgtsMensal", "incluirMultaFgts40", "diasAvisoPrevioInformado")),
                    section("atualizacao", "Atualização", List.of("dataInicioAtualizacao", "dataFimAtualizacao", "fatorPreJudicialIpcae", "taxasSelicMensais", "criterioAtualizacaoNome", "criterioJurosNome")),
                    section("penalidades", "Penalidades e encargos", List.of("aplicarMultaArt467", "aplicarMultaArt477", "percentualHonorariosSucumbenciais", "incluirInssSegurado", "percentualInssSegurado", "incluirIrrf", "percentualIrrfEfetivo")),
                    section("observacoes", "Observações", List.of("parcelasLivres", "observacoesTecnicas", "nomeSolicitante", "registroProfissionalSolicitante"))
            );
            case "FAZENDA_TRIBUTARIO" -> List.of(
                    section("dados_processo", "Dados do processo", List.of("tituloCalculo", "numeroProcesso", "enteTributante", "tributo", "principal", "vencimento", "dataCalculo")),
                    section("mora_indices", "Correção, juros e índices", List.of("percentualMultaMoraDiaria", "limitePercentualMultaMora", "aplicarMaisUmPorCentoNoMesPagamento", "taxasSelicMensais", "criterioCorrecaoMonetariaNome", "criterioJurosNome", "dataInicioJurosMora", "aplicarProRataDie")),
                    section("descontos", "Multas, descontos e abatimentos", List.of("percentualMultaOficio", "percentualReducaoMulta", "percentualDescontoPrograma", "valorGarantidoOuDepositado", "creditosCompensaveis")),
                    section("encargos", "Encargos e honorários", List.of("percentualEncargoLegal", "percentualHonorarios", "custas")),
                    section("observacoes", "Observações", List.of("observacoesTecnicas", "nomeSolicitante", "registroProfissionalSolicitante"))
            );
            case "CUSTAS_PROCESSUAIS" -> List.of(
                    section("dados_basicos", "Dados básicos", List.of("tituloCalculo", "numeroProcesso", "tribunal", "sistemaOrigem", "classeProcessual", "valorCausa")),
                    section("taxa_preparo", "Taxa e preparo", List.of("percentualTaxaJudiciaria", "valorMinimoTaxaJudiciaria", "percentualPreparoRecursal", "unidadeReferenciaNome", "valorUnidadeReferencia")),
                    section("despesas_processuais", "Despesas processuais", List.of("despesasPostais", "diligenciasOficialJustica", "despesasEditais", "pesquisasConveniadas", "porteRemessaRetorno", "custasFinaisComplementares")),
                    section("atualizacao", "Atualização", List.of("fatorAtualizacaoCustas", "dataBaseCalculo", "dataFinalCalculo")),
                    section("abatimentos_deposito", "Abatimentos e depósito", List.of("depositoJudicialVinculado")),
                    section("observacoes", "Observações", List.of("observacoesTecnicas", "nomeSolicitante", "registroProfissionalSolicitante"))
            );
            case "FEDERAL_PREVIDENCIARIO_CJF" -> List.of(
                    section("dados_beneficio", "Dados do benefício", List.of("tituloCalculo", "numeroProcesso", "tribunal", "sistemaOrigem", "tipoBeneficio", "rendaMensalAtual")),
                    section("marco_temporal", "Marco temporal", List.of("dib", "dip", "dcb", "dataAjuizamento", "dataCitacao", "dataCalculo", "aplicarPrescricaoQuinquenal")),
                    section("parcelas_abono", "Parcelas e abono", List.of("incluirAbonoAnual", "parcelasPagasAdministrativamente", "parcelasPagasPorTutela")),
                    section("atualizacao_juros", "Atualização e juros", List.of("taxasCorrecaoMensais", "fatorCorrecaoMonetaria", "percentualJurosMoraMensal", "criterioAtualizacaoNome", "criterioJurosNome")),
                    section("honorarios_pagamento", "Honorários e classificação do pagamento", List.of("percentualHonorarios", "salarioMinimoReferencia", "tetoRpvEmSalariosMinimos")),
                    section("observacoes", "Observações", List.of("observacoesTecnicas", "nomeSolicitante", "registroProfissionalSolicitante"))
            );
            default -> List.of();
        };
    }

    private List<Map<String, Object>> fields(String dominio) {
        return switch (dominio) {
            case "TRABALHISTA_CLT" -> List.of(
                    field("tituloCalculo", "string", false, "Título do cálculo", "dados_iniciais", null, "Cálculo rescisório padrão"),
                    field("numeroProcesso", "string", false, "Número do processo", "dados_iniciais", null, "0000000-00.2026.5.00.0000"),
                    field("salarioBase", "decimal", true, "Salário-base", "dados_iniciais", "0.00", "3200.00"),
                    field("admissao", "date", true, "Data de admissão", "dados_iniciais", null, "2022-01-10"),
                    field("demissao", "date", true, "Data de demissão", "dados_iniciais", null, "2026-03-20"),
                    field("tipoDispensa", "string", false, "Tipo de dispensa", "dados_iniciais", null, "DISPENSA_SEM_JUSTA_CAUSA"),
                    field("cargaHorariaMensalBase", "decimal", false, "Carga horária mensal", "jornada_verbas", "220", "220"),
                    field("quantidadeHorasExtras50", "decimal", false, "Horas extras 50%", "jornada_verbas", "0", "18"),
                    field("quantidadeHorasExtras100", "decimal", false, "Horas extras 100%", "jornada_verbas", "0", "4"),
                    field("quantidadeHorasIntervaloIntrajornada", "decimal", false, "Intervalo intrajornada suprimido", "jornada_verbas", "0", "3"),
                    field("quantidadeHorasNoturnas", "decimal", false, "Horas noturnas", "jornada_verbas", "0", "12"),
                    field("incluirReflexosEmFeriasDecimoTerceiroFgts", "boolean", false, "Incluir reflexos", "reflexos_fgts", "true", "true"),
                    field("incluirFgtsMensal", "boolean", false, "Incluir FGTS mensal", "reflexos_fgts", "true", "true"),
                    field("incluirMultaFgts40", "boolean", false, "Incluir multa de 40%", "reflexos_fgts", "true", "true"),
                    field("taxasSelicMensais", "array", false, "Série SELIC mensal", "atualizacao", null, null),
                    field("parcelasLivres", "array", false, "Parcelas livres", "observacoes", null, null),
                    field("observacoesTecnicas", "string", false, "Observações técnicas", "observacoes", null, "Critério pericial alinhado ao caso."),
                    field("nomeSolicitante", "string", false, "Nome do solicitante", "observacoes", null, "Fulano da Silva"),
                    field("registroProfissionalSolicitante", "string", false, "Registro profissional", "observacoes", null, "OAB/CE 12345")
            );
            case "FAZENDA_TRIBUTARIO" -> List.of(
                    field("tituloCalculo", "string", false, "Título do cálculo", "dados_processo", null, "Cálculo tributário padrão"),
                    field("numeroProcesso", "string", false, "Número do processo", "dados_processo", null, "0000000-00.2026.4.00.0000"),
                    field("principal", "decimal", true, "Valor principal", "dados_processo", "0.00", "15000.00"),
                    field("vencimento", "date", true, "Data de vencimento", "dados_processo", null, "2025-11-30"),
                    field("dataCalculo", "date", true, "Data do cálculo", "dados_processo", null, "2026-03-29"),
                    field("percentualMultaMoraDiaria", "decimal", false, "Multa de mora diária", "mora_indices", "0.0033", "0.0033"),
                    field("limitePercentualMultaMora", "decimal", false, "Teto de multa", "mora_indices", "0.20", "0.20"),
                    field("aplicarMaisUmPorCentoNoMesPagamento", "boolean", false, "Aplicar 1% no mês do pagamento", "mora_indices", "true", "true"),
                    field("taxasSelicMensais", "array", false, "Série SELIC mensal", "mora_indices", null, null),
                    field("percentualMultaOficio", "decimal", false, "Multa de ofício", "descontos", "0.00", "0.00"),
                    field("percentualReducaoMulta", "decimal", false, "Redução de multa", "descontos", "0.00", "0.00"),
                    field("percentualDescontoPrograma", "decimal", false, "Desconto de programa", "descontos", "0.00", "0.00"),
                    field("valorGarantidoOuDepositado", "decimal", false, "Valor garantido ou depositado", "descontos", "0.00", "2500.00"),
                    field("creditosCompensaveis", "array", false, "Créditos compensáveis", "descontos", null, null),
                    field("percentualEncargoLegal", "decimal", false, "Encargo legal", "encargos", "0.00", "0.10"),
                    field("percentualHonorarios", "decimal", false, "Honorários", "encargos", "0.00", "0.10"),
                    field("custas", "decimal", false, "Custas", "encargos", "0.00", "350.00"),
                    field("observacoesTecnicas", "string", false, "Observações técnicas", "observacoes", null, "SELIC acumulada conforme memória informada."),
                    field("nomeSolicitante", "string", false, "Nome do solicitante", "observacoes", null, "Procuradoria X"),
                    field("registroProfissionalSolicitante", "string", false, "Registro profissional", "observacoes", null, "Matrícula 123456")
            );
            case "CUSTAS_PROCESSUAIS" -> List.of(
                    field("tituloCalculo", "string", false, "Título do cálculo", "dados_basicos", null, "Custas processuais padrão"),
                    field("numeroProcesso", "string", false, "Número do processo", "dados_basicos", null, "0000000-00.2026.8.26.0000"),
                    field("tribunal", "string", false, "Tribunal", "dados_basicos", null, "TJSP"),
                    field("sistemaOrigem", "string", false, "Sistema de origem", "dados_basicos", null, "e-SAJ"),
                    field("classeProcessual", "string", false, "Classe processual", "dados_basicos", null, "Apelação"),
                    field("valorCausa", "decimal", true, "Valor da causa", "dados_basicos", "0.00", "50000.00"),
                    field("percentualTaxaJudiciaria", "decimal", false, "Percentual da taxa judiciária", "taxa_preparo", "0.015", "0.015"),
                    field("valorMinimoTaxaJudiciaria", "decimal", false, "Valor mínimo da taxa", "taxa_preparo", "0.00", "192.10"),
                    field("percentualPreparoRecursal", "decimal", false, "Percentual do preparo recursal", "taxa_preparo", "0.00", "0.04"),
                    field("unidadeReferenciaNome", "string", false, "Nome da unidade de referência", "taxa_preparo", null, "UFESP"),
                    field("valorUnidadeReferencia", "decimal", false, "Valor da unidade de referência", "taxa_preparo", "0.00", "38.42"),
                    field("despesasPostais", "decimal", false, "Despesas postais", "despesas", "0.00", "120.00"),
                    field("diligenciasOficialJustica", "decimal", false, "Diligências de oficial de justiça", "despesas", "0.00", "95.00"),
                    field("despesasEditais", "decimal", false, "Despesas com editais", "despesas", "0.00", "0.00"),
                    field("pesquisasConveniadas", "decimal", false, "Pesquisas conveniadas", "despesas", "0.00", "45.00"),
                    field("porteRemessaRetorno", "decimal", false, "Porte remessa/retorno", "despesas", "0.00", "0.00"),
                    field("custasFinaisComplementares", "decimal", false, "Custas finais complementares", "despesas", "0.00", "0.00"),
                    field("dataBaseCalculo", "date", false, "Data-base", "atualizacao", null, "2026-03-01"),
                    field("dataFinalCalculo", "date", false, "Data final", "atualizacao", null, "2026-03-29"),
                    field("fatorAtualizacaoCustas", "decimal", false, "Fator de atualização", "atualizacao", "0.00", "0.0120"),
                    field("depositoJudicialVinculado", "decimal", false, "Depósito judicial vinculado", "abatimentos", "0.00", "500.00"),
                    field("observacoesTecnicas", "string", false, "Observações técnicas", "abatimentos", null, "Guia de preparo e diligências conforme tabela local."),
                    field("nomeSolicitante", "string", false, "Nome do solicitante", "abatimentos", null, "Fulano da Silva"),
                    field("registroProfissionalSolicitante", "string", false, "Registro profissional", "abatimentos", null, "OAB/SP 123456")
            );
            default -> List.of();
        };
    }


    private Map<String, Object> financialAiRequestExample(String dominio, CalculoJudicialSolicitantePerfil perfil) {
        return ordered(
                "agentCode", "IA_FINANCEIRA_PJB",
                "route", CalculoJudicialDomainSupport.financialAiExecuteRoute(),
                "presetDomain", dominio,
                "profile", perfil.name(),
                "mode", "assistir_e_executar",
                "sourceOfTruth", "CALCULADORA_REAL",
                "executionModel", "planner_router_normalizer_validator_executor_verifier",
                "commandShape", ordered(
                        "dominio", dominio,
                        "pedidoUsuario", "Calcule e valide com segurança.",
                        "executionProfile", "default_2026",
                        "payload", defaultPayload(dominio, perfil)
                ),
                "guardrails", CalculatorHelpMessages.iaGuardrails()
        );
    }

    private Map<String, Object> resultContract(String dominio) {
        return ordered(
                "jsonContentType", "application/json",
                "pdfContentType", "application/pdf",
                "totais", List.of("subtotalPrincipal", "subtotalAtualizacao", "subtotalAcessorios", "totalGeral"),
                "listas", List.of("itens", "alertas", "fundamentos", "trilhaAuditoria"),
                "metadataBindings", frontendContractService.frontendBindings(dominio),
                "officialTablesRoute", CalculoJudicialDomainSupport.officialTablesRoute(dominio),
                "officialTablesProfile", tabelaOficialService.profile(dominio),
                "pdfHeaders", CalculoJudicialDomainSupport.httpContract(dominio).get("pdfHeaders"),
                "observabilityHeaders", CalculoJudicialDomainSupport.httpContract(dominio).get("observabilityHeaders"),
                "bootstrapRoute", CalculoJudicialDomainSupport.bootstrapRoute(dominio),
                "financialAiRoute", CalculoJudicialDomainSupport.financialAiExecuteRoute(),
                "financialAiPresetDomain", dominio,
                "financialAiExecutionModel", "planner_router_normalizer_validator_executor_verifier",
                "aiAgents", frontendContractService.aiAgentsCatalog()
        );
    }

    private Map<String, Object> uxContract(String dominio, CalculoJudicialSolicitantePerfil perfil) {
        return ordered(
                "tabs", List.of("Visão geral", CalculoJudicialDomainSupport.aba(dominio), "Ajuda", "IA assistiva"),
                "quickStartJourney", CalculatorHelpMessages.quickStartJourney(dominio, perfil),
                "liveExperience", CalculatorHelpMessages.liveComponentDesign(dominio, perfil),
                "workspaceSignals", CalculatorHelpMessages.smartWorkspaceSignals(perfil),
                "completionMessages", CalculatorHelpMessages.completionMessages(dominio, perfil),
                "safeAutomationCapabilities", CalculatorHelpMessages.safeAutomationCapabilities(),
                "guardrailsIa", CalculatorHelpMessages.iaGuardrails(),
                "officialTablesRoute", CalculoJudicialDomainSupport.officialTablesRoute(dominio),
                "officialTablesProfile", tabelaOficialService.profile(dominio),
                "officialBenchmarkCoverage", CalculatorHelpMessages.officialBenchmarkSignals(),
                "expansionIdeas", CalculatorHelpMessages.expansionIdeas(),
                "profileCapabilities", frontendContractService.profileCapabilities(perfil),
                "financialIaMessages", CalculatorHelpMessages.financialIaMessages(),
                "contractVersion", frontendContractService.version(),
                "contractFingerprint", frontendContractService.fingerprint()
        );
    }

    private Map<String, Object> domainErrorContract(String dominio) {
        return ordered(
                "domain", dominio,
                "problemDetail", Boolean.TRUE,
                "types", List.of("unsupported_domain", "bad_request", "validation_error", "constraint_violation", "unsupported_media_type", "method_not_allowed", "rate_limited", "business_rule", "internal_error"),
                "fieldErrorsPath", "fieldErrors",
                "catalogRoute", CalculoJudicialDomainSupport.catalogRoute(dominio),
                "bootstrapRoute", CalculoJudicialDomainSupport.bootstrapRoute(dominio),
                "officialTablesRoute", CalculoJudicialDomainSupport.officialTablesRoute(dominio),
                "financialAiRoute", CalculoJudicialDomainSupport.financialAiExecuteRoute(),
                "financialAiPresetDomain", dominio,
                "financialAiExecutionModel", "planner_router_normalizer_validator_executor_verifier",
                "supportedDomainsPath", "supportedDomains",
                "contractVersion", frontendContractService.version(),
                "contractFingerprint", frontendContractService.fingerprint()
        );
    }

    private Map<String, Object> defaultPayload(String dominio, CalculoJudicialSolicitantePerfil perfil) {
        return switch (dominio) {
            case "TRABALHISTA_CLT" -> ordered(
                    "tituloCalculo", "Cálculo trabalhista CLT PJB",
                    "numeroProcesso", null,
                    "perfilSolicitante", perfil.name(),
                    "nomeSolicitante", null,
                    "registroProfissionalSolicitante", null,
                    "salarioBase", "0.00",
                    "admissao", null,
                    "demissao", null,
                    "tipoDispensa", "DISPENSA_SEM_JUSTA_CAUSA",
                    "cargaHorariaMensalBase", "220",
                    "quantidadeHorasExtras50", "0",
                    "quantidadeHorasExtras100", "0",
                    "quantidadeHorasIntervaloIntrajornada", "0",
                    "quantidadeHorasNoturnas", "0",
                    "incluirReflexosEmFeriasDecimoTerceiroFgts", true,
                    "incluirFgtsMensal", true,
                    "incluirMultaFgts40", true,
                    "aplicarMultaArt467", false,
                    "aplicarMultaArt477", false,
                    "parcelasLivres", List.of(),
                    "taxasSelicMensais", List.of(),
                    "observacoesTecnicas", null
            );
            case "FAZENDA_TRIBUTARIO" -> ordered(
                    "tituloCalculo", "Cálculo fazenda e tributário PJB",
                    "numeroProcesso", null,
                    "perfilSolicitante", perfil.name(),
                    "nomeSolicitante", null,
                    "registroProfissionalSolicitante", null,
                    "principal", "0.00",
                    "vencimento", null,
                    "dataCalculo", null,
                    "percentualMultaMoraDiaria", "0.0033",
                    "limitePercentualMultaMora", "0.20",
                    "percentualMultaOficio", "0.00",
                    "percentualReducaoMulta", "0.00",
                    "percentualDescontoPrograma", "0.00",
                    "aplicarMaisUmPorCentoNoMesPagamento", true,
                    "aplicarProRataDie", false,
                    "valorGarantidoOuDepositado", "0.00",
                    "creditosCompensaveis", List.of(),
                    "taxasSelicMensais", List.of(),
                    "observacoesTecnicas", null
            );
            case "CUSTAS_PROCESSUAIS" -> ordered(
                    "tituloCalculo", "Cálculo de custas e despesas PJB",
                    "numeroProcesso", null,
                    "perfilSolicitante", perfil.name(),
                    "nomeSolicitante", null,
                    "registroProfissionalSolicitante", null,
                    "tribunal", null,
                    "sistemaOrigem", null,
                    "classeProcessual", null,
                    "valorCausa", "0.00",
                    "percentualTaxaJudiciaria", "0.015",
                    "valorMinimoTaxaJudiciaria", "0.00",
                    "percentualPreparoRecursal", "0.00",
                    "despesasPostais", "0.00",
                    "diligenciasOficialJustica", "0.00",
                    "despesasEditais", "0.00",
                    "pesquisasConveniadas", "0.00",
                    "porteRemessaRetorno", "0.00",
                    "custasFinaisComplementares", "0.00",
                    "depositoJudicialVinculado", "0.00",
                    "fatorAtualizacaoCustas", "0.00",
                    "dataBaseCalculo", null,
                    "dataFinalCalculo", null,
                    "unidadeReferenciaNome", null,
                    "valorUnidadeReferencia", "0.00",
                    "observacoesTecnicas", null
            );
            case "FEDERAL_PREVIDENCIARIO_CJF" -> ordered(
                    "tituloCalculo", "Cálculo federal previdenciário CJF PJB",
                    "numeroProcesso", null,
                    "perfilSolicitante", perfil.name(),
                    "nomeSolicitante", null,
                    "registroProfissionalSolicitante", null,
                    "tribunal", "TRF5",
                    "sistemaOrigem", "Creta/JEF",
                    "tipoBeneficio", "Auxílio por incapacidade temporária",
                    "rendaMensalAtual", "0.00",
                    "dib", null,
                    "dip", null,
                    "dcb", null,
                    "dataAjuizamento", null,
                    "dataCitacao", null,
                    "dataCalculo", null,
                    "aplicarPrescricaoQuinquenal", true,
                    "incluirAbonoAnual", true,
                    "parcelasPagasAdministrativamente", "0.00",
                    "parcelasPagasPorTutela", "0.00",
                    "taxasCorrecaoMensais", List.of(),
                    "fatorCorrecaoMonetaria", "0.00",
                    "percentualJurosMoraMensal", "0.0050",
                    "percentualHonorarios", "0.10",
                    "salarioMinimoReferencia", salarioMinimoNacionalService.valorVigente().toPlainString(),
                    "tetoRpvEmSalariosMinimos", "60",
                    "observacoesTecnicas", null
            );
            default -> Map.of();
        };
    }

    private Map<String, Object> requestExample(String dominio, CalculoJudicialSolicitantePerfil perfil) {
        return switch (dominio) {
            case "TRABALHISTA_CLT" -> ordered(
                    "tituloCalculo", "Rescisão padrão com extras e FGTS",
                    "numeroProcesso", "0001234-56.2026.5.07.0001",
                    "perfilSolicitante", perfil.name(),
                    "nomeSolicitante", "Fulano da Silva",
                    "registroProfissionalSolicitante", perfil == CalculoJudicialSolicitantePerfil.ADVOGADO ? "OAB/CE 12345" : "Matrícula 1001",
                    "salarioBase", "3200.00",
                    "admissao", "2022-01-10",
                    "demissao", "2026-03-20",
                    "tipoDispensa", "DISPENSA_SEM_JUSTA_CAUSA",
                    "cargaHorariaMensalBase", "220",
                    "quantidadeHorasExtras50", "18",
                    "quantidadeHorasExtras100", "4",
                    "quantidadeHorasIntervaloIntrajornada", "3",
                    "quantidadeHorasNoturnas", "12",
                    "percentualAdicionalNoturno", "0.20",
                    "outrasParcelasFixasMensais", "450.00",
                    "incluirReflexosEmFeriasDecimoTerceiroFgts", true,
                    "incluirFgtsMensal", true,
                    "incluirMultaFgts40", true,
                    "aplicarMultaArt467", false,
                    "aplicarMultaArt477", true,
                    "percentualHonorariosSucumbenciais", "0.10",
                    "taxasSelicMensais", List.of(
                            ordered("competencia", "2026-01", "indice", "0.0090"),
                            ordered("competencia", "2026-02", "indice", "0.0085")
                    ),
                    "parcelasLivres", List.of(
                            ordered("descricao", "Comissão variável", "valor", "380.00")
                    ),
                    "observacoesTecnicas", "Memória inicial para conferência pericial."
            );
            case "FAZENDA_TRIBUTARIO" -> ordered(
                    "tituloCalculo", "Débito federal com SELIC e abatimentos",
                    "numeroProcesso", "0009876-00.2026.4.05.0001",
                    "perfilSolicitante", perfil.name(),
                    "nomeSolicitante", "Procuradoria X",
                    "registroProfissionalSolicitante", "Matrícula 123456",
                    "enteTributante", "União",
                    "tributo", "PIS/COFINS",
                    "principal", "15000.00",
                    "vencimento", "2025-11-30",
                    "dataCalculo", "2026-03-29",
                    "percentualMultaMoraDiaria", "0.0033",
                    "limitePercentualMultaMora", "0.20",
                    "percentualMultaOficio", "0.00",
                    "percentualReducaoMulta", "0.10",
                    "percentualDescontoPrograma", "0.05",
                    "aplicarMaisUmPorCentoNoMesPagamento", true,
                    "aplicarProRataDie", false,
                    "valorGarantidoOuDepositado", "2500.00",
                    "percentualEncargoLegal", "0.10",
                    "percentualHonorarios", "0.10",
                    "custas", "350.00",
                    "taxasSelicMensais", List.of(
                            ordered("competencia", "2025-12", "indice", "0.0094"),
                            ordered("competencia", "2026-01", "indice", "0.0089"),
                            ordered("competencia", "2026-02", "indice", "0.0085")
                    ),
                    "creditosCompensaveis", List.of(
                            ordered("descricao", "Crédito reconhecido", "valor", "1000.00")
                    ),
                    "observacoesTecnicas", "Aplicação de desconto de programa e abatimento de garantia."
            );
            case "CUSTAS_PROCESSUAIS" -> ordered(
                    "tituloCalculo", "Custas recursais com diligências e depósito parcial",
                    "numeroProcesso", "1000001-23.2026.8.26.0100",
                    "perfilSolicitante", perfil.name(),
                    "nomeSolicitante", "Fulano da Silva",
                    "registroProfissionalSolicitante", perfil == CalculoJudicialSolicitantePerfil.ADVOGADO ? "OAB/SP 54321" : "Matrícula 2001",
                    "tribunal", "TJSP",
                    "sistemaOrigem", "e-SAJ",
                    "classeProcessual", "Apelação Cível",
                    "valorCausa", "50000.00",
                    "percentualTaxaJudiciaria", "0.0150",
                    "valorMinimoTaxaJudiciaria", "0.00",
                    "percentualPreparoRecursal", "0.0400",
                    "despesasPostais", "85.00",
                    "diligenciasOficialJustica", "120.00",
                    "despesasEditais", "0.00",
                    "pesquisasConveniadas", "55.00",
                    "porteRemessaRetorno", "0.00",
                    "custasFinaisComplementares", "0.00",
                    "depositoJudicialVinculado", "500.00",
                    "fatorAtualizacaoCustas", "0.0160",
                    "dataBaseCalculo", "2026-01-10",
                    "dataFinalCalculo", "2026-03-29",
                    "unidadeReferenciaNome", "UFESP",
                    "valorUnidadeReferencia", "38.42",
                    "observacoesTecnicas", "Separação de taxa, preparo, diligências e abatimento por depósito judicial."
            );
            case "FEDERAL_PREVIDENCIARIO_CJF" -> ordered(
                    "tituloCalculo", "Atrasados previdenciários federais com compensações",
                    "numeroProcesso", "0801234-56.2026.4.05.8300",
                    "perfilSolicitante", perfil.name(),
                    "nomeSolicitante", "Fulano da Silva",
                    "registroProfissionalSolicitante", perfil == CalculoJudicialSolicitantePerfil.ADVOGADO ? "OAB/PE 12345" : "Matrícula 2001",
                    "tribunal", "TRF5",
                    "sistemaOrigem", "Creta/JEF",
                    "tipoBeneficio", "Auxílio por incapacidade temporária",
                    "rendaMensalAtual", "1812.00",
                    "dib", "2021-05-14",
                    "dip", "2026-02-01",
                    "dataAjuizamento", "2022-08-10",
                    "dataCitacao", "2022-09-05",
                    "dataCalculo", "2026-03-29",
                    "aplicarPrescricaoQuinquenal", true,
                    "incluirAbonoAnual", true,
                    "parcelasPagasAdministrativamente", "3500.00",
                    "parcelasPagasPorTutela", "1200.00",
                    "fatorCorrecaoMonetaria", "0.1280",
                    "percentualJurosMoraMensal", "0.0050",
                    "percentualHonorarios", "0.10",
                    "salarioMinimoReferencia", salarioMinimoNacionalService.valorVigente().toPlainString(),
                    "tetoRpvEmSalariosMinimos", "60",
                    "criterioAtualizacaoNome", "Manual CJF e tabela institucional",
                    "criterioJurosNome", "Juros de mora parametrizados",
                    "taxasCorrecaoMensais", List.of(
                            ordered("competencia", "2025-12", "indice", "0.0045"),
                            ordered("competencia", "2026-01", "indice", "0.0042"),
                            ordered("competencia", "2026-02", "indice", "0.0040")
                    ),
                    "observacoesTecnicas", "Aplicação de corte quinquenal e abatimento de parcelas pagas."
            );
            default -> Map.of();
        };
    }

    private Map<String, Object> responseExample(String dominio, CalculoJudicialSolicitantePerfil perfil) {
        return switch (dominio) {
            case "TRABALHISTA_CLT" -> ordered(
                    "dominio", dominio,
                    "titulo", "Rescisão padrão com extras e FGTS",
                    "numeroProcesso", "0001234-56.2026.5.07.0001",
                    "perfilSolicitante", perfil.name(),
                    "subtotalPrincipal", "19840.32",
                    "subtotalAtualizacao", "640.55",
                    "subtotalAcessorios", "7320.11",
                    "totalGeral", "27800.98",
                    "itens", List.of(
                            ordered("codigo", "SALDO_SALARIO", "titulo", "Saldo de salário", "valor", "3520.00"),
                            ordered("codigo", "HORAS_EXTRAS_50", "titulo", "Horas extras 50%", "valor", "392.73")
                    ),
                    "alertas", List.of("Confira a base do adicional noturno e o extrato real de FGTS."),
                    "metadata", ordered(
                            "workflowState", "READY",
                            "pdfFilenameSuggested", "pjb-calculo-trabalhista-clt-fulano-da-silva-0001234-56-2026-5-07-0001.pdf",
                            "frontendCatalogRoute", CalculoJudicialDomainSupport.catalogRoute(dominio),
                            "readyNotification", ordered("title", "Cálculo concluído", "status", "success"),
                            "apiRoutes", CalculoJudicialDomainSupport.apiRoutes(dominio),
                            "contractVersion", frontendContractService.version(),
                            "contractFingerprint", frontendContractService.fingerprint()
                    )
            );
            case "FAZENDA_TRIBUTARIO" -> ordered(
                    "dominio", dominio,
                    "titulo", "Débito federal com SELIC e abatimentos",
                    "numeroProcesso", "0009876-00.2026.4.05.0001",
                    "perfilSolicitante", perfil.name(),
                    "subtotalPrincipal", "15000.00",
                    "subtotalAtualizacao", "1875.44",
                    "subtotalAcessorios", "2135.00",
                    "totalGeral", "19010.44",
                    "itens", List.of(
                            ordered("codigo", "PRINCIPAL", "titulo", "Principal", "valor", "15000.00"),
                            ordered("codigo", "MULTA_MORA", "titulo", "Multa de mora", "valor", "990.00")
                    ),
                    "alertas", List.of("Valide a série SELIC oficial antes da emissão definitiva do PDF."),
                    "metadata", ordered(
                            "workflowState", "READY",
                            "pdfFilenameSuggested", "pjb-calculo-fazenda-tributario-procuradoria-x-0009876-00-2026-4-05-0001.pdf",
                            "frontendCatalogRoute", CalculoJudicialDomainSupport.catalogRoute(dominio),
                            "readyNotificationIaAssistida", ordered("title", "Cálculo concluído com apoio da IA assistiva", "status", "success"),
                            "apiRoutes", CalculoJudicialDomainSupport.apiRoutes(dominio),
                            "contractVersion", frontendContractService.version(),
                            "contractFingerprint", frontendContractService.fingerprint()
                    )
            );
            case "CUSTAS_PROCESSUAIS" -> ordered(
                    "dominio", dominio,
                    "titulo", "Custas recursais com diligências e depósito parcial",
                    "numeroProcesso", "1000001-23.2026.8.26.0100",
                    "perfilSolicitante", perfil.name(),
                    "subtotalPrincipal", "750.00",
                    "subtotalAtualizacao", "12.00",
                    "subtotalAcessorios", "1760.00",
                    "totalGeral", "2522.00",
                    "itens", List.of(
                            ordered("codigo", "TAXA_JUDICIARIA", "titulo", "Taxa judiciária projetada", "valor", "750.00"),
                            ordered("codigo", "PREPARO_RECURSAL", "titulo", "Preparo recursal projetado", "valor", "2000.00"),
                            ordered("codigo", "DEPOSITO_VINCULADO", "titulo", "Depósito judicial vinculado", "valor", "-500.00")
                    ),
                    "alertas", List.of("Confira a tabela vigente do tribunal e a unidade de referência local antes da emissão definitiva da guia."),
                    "metadata", ordered(
                            "workflowState", "READY",
                            "pdfFilenameSuggested", "pjb-calculo-custas-processuais-fulano-da-silva-1000001-23-2026-8-26-0100.pdf",
                            "frontendCatalogRoute", CalculoJudicialDomainSupport.catalogRoute(dominio),
                            "readyNotification", ordered("title", "Cálculo concluído", "status", "success"),
                            "apiRoutes", CalculoJudicialDomainSupport.apiRoutes(dominio),
                            "contractVersion", frontendContractService.version(),
                            "contractFingerprint", frontendContractService.fingerprint()
                    )
            );
            case "FEDERAL_PREVIDENCIARIO_CJF" -> ordered(
                    "dominio", dominio,
                    "titulo", "Atrasados previdenciários federais com compensações",
                    "numeroProcesso", "0801234-56.2026.4.05.8300",
                    "perfilSolicitante", perfil.name(),
                    "subtotalPrincipal", "12420.00",
                    "subtotalAtualizacao", "1855.40",
                    "subtotalAcessorios", "-338.46",
                    "totalGeral", "13936.94",
                    "itens", List.of(
                            ordered("codigo", "PARCELAS_VENCIDAS", "titulo", "Parcelas vencidas do benefício", "valor", "10872.00"),
                            ordered("codigo", "ABONO_ANUAL", "titulo", "Abono anual projetado sobre atrasados", "valor", "1548.00"),
                            ordered("codigo", "ABATIMENTO_ADMINISTRATIVO", "titulo", "Abatimento por parcelas administrativas pagas", "valor", "-3500.00")
                    ),
                    "alertas", List.of("Valide o marco prescricional, a série de correção e a classificação entre RPV e precatório antes da emissão definitiva do PDF."),
                    "metadata", ordered(
                            "workflowState", "READY",
                            "pdfFilenameSuggested", "pjb-calculo-federal-previdenciario-cjf-fulano-da-silva-0801234-56-2026-4-05-8300.pdf",
                            "frontendCatalogRoute", CalculoJudicialDomainSupport.catalogRoute(dominio),
                            "readyNotification", ordered("title", "Cálculo concluído", "status", "success"),
                            "classificacaoPagamento", "RPV",
                            "apiRoutes", CalculoJudicialDomainSupport.apiRoutes(dominio),
                            "contractVersion", frontendContractService.version(),
                            "contractFingerprint", frontendContractService.fingerprint()
                    )
            );
            default -> Map.of();
        };
    }

    private Map<String, Object> errorExample(String dominio) {
        return ordered(
                "type", "https://pjb.local/problems/unsupported_domain",
                "title", "Unprocessable Entity",
                "status", 422,
                "detail", "Domínio de cálculo não suportado para esta operação.",
                "supportedDomains", CalculoJudicialDomainSupport.supportedDomains(),
                "frontendCatalogRoute", CalculoJudicialDomainSupport.catalogRoute(),
                "frontendBootstrapRoute", CalculoJudicialDomainSupport.bootstrapRoute(dominio),
                "domainHint", dominio,
                "transport", "application/problem+json",
                "contractVersion", frontendContractService.version(),
                "contractFingerprint", frontendContractService.fingerprint()
        );
    }

    private Map<String, Object> section(String codigo, String titulo, List<String> fields) {
        return ordered(
                "codigo", codigo,
                "titulo", titulo,
                "campos", fields
        );
    }

    private Map<String, Object> field(String name,
                                      String type,
                                      boolean required,
                                      String label,
                                      String section,
                                      String defaultValue,
                                      String exampleValue) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("type", type);
        map.put("required", required);
        map.put("label", label);
        map.put("section", section);
        if (defaultValue != null) {
            map.put("defaultValue", defaultValue);
        }
        if (exampleValue != null) {
            map.put("exampleValue", exampleValue);
        }
        return java.util.Collections.unmodifiableMap(map);
    }

    private Map<String, Object> ordered(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}
