package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialAssistenciaResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import com.tcc.pjb.backend.model.dto.processual.calculo.CustasProcessuaisCalculoAvancadoRequest;
import com.tcc.pjb.backend.model.dto.processual.calculo.FazendaTributarioCalculoAvancadoRequest;
import com.tcc.pjb.backend.model.dto.processual.calculo.FederalPrevidenciarioCjfCalculoAvancadoRequest;
import com.tcc.pjb.backend.model.dto.processual.calculo.TrabalhistaCalculoAvancadoRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CalculoJudicialAssistenciaService {

    private final CalculoJudicialProfileResolverService profileResolverService;
    private final CalculoJudicialFrontendContractService frontendContractService;

    public CalculoJudicialAssistenciaService(CalculoJudicialProfileResolverService profileResolverService,
                                             CalculoJudicialFrontendContractService frontendContractService) {
        this.profileResolverService = Objects.requireNonNull(profileResolverService);
        this.frontendContractService = Objects.requireNonNull(frontendContractService);
    }

    public CalculoJudicialAssistenciaResponse orientarTrabalhista(TrabalhistaCalculoAvancadoRequest request, Authentication authentication) {
        CalculoJudicialSolicitantePerfil perfil = profileResolverService.resolve(authentication, request == null ? null : request.perfilSolicitante());
        List<String> pendentes = new ArrayList<>();
        List<String> bloqueios = new ArrayList<>();
        List<String> ajustes = new ArrayList<>();
        List<String> proximosPassos = new ArrayList<>();
        Map<String, Object> autopreenchimento = new LinkedHashMap<>();
        Map<String, Object> desenhoAssistido = desenhoBase("TRABALHISTA_CLT", perfil);

        if (request == null || request.salarioBase() == null) {
            pendentes.add("Informar salário-base.");
        }
        if (request == null || request.admissao() == null) {
            pendentes.add("Informar data de admissão.");
        }
        if (request == null || request.demissao() == null) {
            pendentes.add("Informar data de demissão.");
        }
        if (request != null && request.admissao() != null && request.demissao() != null) {
            if (request.demissao().isBefore(request.admissao())) {
                bloqueios.add("A data de demissão não pode ser anterior à data de admissão.");
            } else {
                long meses = ChronoUnit.MONTHS.between(request.admissao().withDayOfMonth(1), request.demissao().withDayOfMonth(1)) + 1;
                autopreenchimento.put("competenciasEstimadas", meses);
                autopreenchimento.put("avosEstimados", CalculoJudicialMath.avosTrabalhistas(request.admissao(), request.demissao()));
                ajustes.add("A IA assistiva consegue preencher automaticamente competências e avos com base nas datas informadas.");
            }
        }
        BigDecimal cargaPadrao = request != null && request.cargaHorariaMensalBase() != null ? request.cargaHorariaMensalBase() : new BigDecimal("220");
        autopreenchimento.put("cargaHorariaMensalSugerida", cargaPadrao);
        if (request == null || request.diasTrabalhadosNoMesRescisao() == null) {
            autopreenchimento.put("diasTrabalhadosNoMesRescisaoSugeridos", 30);
            ajustes.add("Dias trabalhados no mês da rescisão podem iniciar com sugestão conservadora de 30 e ser ajustados pelo usuário.");
        }
        if (request != null && request.tipoDispensa() != null && request.tipoDispensa().toLowerCase().contains("sem justa causa")) {
            autopreenchimento.put("incluirFgtsMensalSugerido", Boolean.TRUE);
            autopreenchimento.put("incluirMultaFgts40Sugerido", Boolean.TRUE);
            autopreenchimento.put("incluirAvisoPrevioSugerido", Boolean.TRUE);
            ajustes.add("Pelo tipo de desligamento informado, o sistema sugere ativar aviso prévio, FGTS mensal e multa de 40%.");
        }
        if (request != null && request.quantidadeHorasExtras50() == null && request.quantidadeHorasExtras100() == null && request.quantidadeHorasIntervaloIntrajornada() == null) {
            proximosPassos.add("Definir se o caso terá horas extras, intervalo intrajornada ou ambos.");
        }
        proximosPassos.add("Revisar se o caso exige reflexos em 13º, férias e FGTS.");
        proximosPassos.add("Conferir multas dos artigos 467 e 477 apenas quando houver suporte fático para sua incidência.");
        desenhoAssistido.put("rotaEntradaDireta", CalculoJudicialDomainSupport.apiRoutes("TRABALHISTA_CLT").get("workspace"));
        desenhoAssistido.put("rotaAjuda", CalculoJudicialDomainSupport.apiRoutes("TRABALHISTA_CLT").get("ajuda"));
        desenhoAssistido.put("rotaCalculo", CalculoJudicialDomainSupport.apiRoutes("TRABALHISTA_CLT").get("json"));
        desenhoAssistido.put("rotaPdf", CalculoJudicialDomainSupport.apiRoutes("TRABALHISTA_CLT").get("pdf"));
        desenhoAssistido.putAll(frontendContractService.frontendMeta("TRABALHISTA_CLT", perfil, "assistencia"));
        desenhoAssistido.put("frontendReady", Boolean.TRUE);
        desenhoAssistido.put("liveExperience", CalculatorHelpMessages.liveComponentDesign("TRABALHISTA_CLT", perfil));
        desenhoAssistido.put("quickStartJourney", CalculatorHelpMessages.quickStartJourney("TRABALHISTA_CLT", perfil));

        return new CalculoJudicialAssistenciaResponse(
                "TRABALHISTA_CLT",
                perfil,
                "Assistente operacional trabalhista do PJB",
                perfil.citizenLike()
                        ? "Vou te conduzir campo por campo, sem esconder parâmetros nem alterar valores críticos sozinho."
                        : "A assistência foi preparada para reduzir atrito operacional sem romper a auditabilidade da memória trabalhista.",
                CalculatorHelpMessages.trabalhistaMessages(),
                List.copyOf(pendentes),
                List.copyOf(bloqueios),
                List.copyOf(ajustes),
                List.copyOf(proximosPassos),
                Map.copyOf(autopreenchimento),
                Map.copyOf(desenhoAssistido),
                CalculatorHelpMessages.iaGuardrails(),
                Instant.now()
        );
    }

    public CalculoJudicialAssistenciaResponse orientarFazenda(FazendaTributarioCalculoAvancadoRequest request, Authentication authentication) {
        CalculoJudicialSolicitantePerfil perfil = profileResolverService.resolve(authentication, request == null ? null : request.perfilSolicitante());
        List<String> pendentes = new ArrayList<>();
        List<String> bloqueios = new ArrayList<>();
        List<String> ajustes = new ArrayList<>();
        List<String> proximosPassos = new ArrayList<>();
        Map<String, Object> autopreenchimento = new LinkedHashMap<>();
        Map<String, Object> desenhoAssistido = desenhoBase("FAZENDA_TRIBUTARIO", perfil);

        if (request == null || request.principal() == null) {
            pendentes.add("Informar o valor principal.");
        }
        if (request == null || request.vencimento() == null) {
            pendentes.add("Informar a data de vencimento.");
        }
        if (request == null || request.dataCalculo() == null) {
            pendentes.add("Informar a data do cálculo.");
        }
        if (request != null && request.vencimento() != null && request.dataCalculo() != null) {
            if (request.dataCalculo().isBefore(request.vencimento())) {
                bloqueios.add("A data do cálculo não pode ser anterior ao vencimento para memória de atraso.");
            } else {
                long dias = ChronoUnit.DAYS.between(request.vencimento(), request.dataCalculo());
                autopreenchimento.put("diasAtrasoEstimados", dias);
                ajustes.add("A IA assistiva pode estimar automaticamente os dias de atraso a partir das datas informadas.");
            }
        }
        autopreenchimento.put("percentualMultaMoraDiariaSugerido", request != null && request.percentualMultaMoraDiaria() != null ? request.percentualMultaMoraDiaria() : new BigDecimal("0.003300"));
        autopreenchimento.put("limitePercentualMultaMoraSugerido", request != null && request.limitePercentualMultaMora() != null ? request.limitePercentualMultaMora() : new BigDecimal("0.200000"));
        if (request == null || request.taxasSelicMensais() == null || request.taxasSelicMensais().isEmpty()) {
            proximosPassos.add("Inserir a série SELIC oficial do caso quando a memória exigir atualização acumulada.");
        }
        if (request != null && Boolean.TRUE.equals(request.aplicarMaisUmPorCentoNoMesPagamento())) {
            ajustes.add("O adicional de 1% no mês do pagamento foi mantido habilitado porque já foi informado no pedido.");
        } else {
            ajustes.add("O sistema pode lembrar a incidência de 1% no mês do pagamento sem aplicar automaticamente quando o regime for duvidoso.");
        }
        proximosPassos.add("Revisar se há programa de transação, redução de multa ou valor garantido para abatimento do saldo.");
        desenhoAssistido.put("rotaEntradaDireta", CalculoJudicialDomainSupport.apiRoutes("FAZENDA_TRIBUTARIO").get("workspace"));
        desenhoAssistido.put("rotaAjuda", CalculoJudicialDomainSupport.apiRoutes("FAZENDA_TRIBUTARIO").get("ajuda"));
        desenhoAssistido.put("rotaCalculo", CalculoJudicialDomainSupport.apiRoutes("FAZENDA_TRIBUTARIO").get("json"));
        desenhoAssistido.put("rotaPdf", CalculoJudicialDomainSupport.apiRoutes("FAZENDA_TRIBUTARIO").get("pdf"));
        desenhoAssistido.putAll(frontendContractService.frontendMeta("FAZENDA_TRIBUTARIO", perfil, "assistencia"));
        desenhoAssistido.put("frontendReady", Boolean.TRUE);
        desenhoAssistido.put("liveExperience", CalculatorHelpMessages.liveComponentDesign("FAZENDA_TRIBUTARIO", perfil));
        desenhoAssistido.put("quickStartJourney", CalculatorHelpMessages.quickStartJourney("FAZENDA_TRIBUTARIO", perfil));

        return new CalculoJudicialAssistenciaResponse(
                "FAZENDA_TRIBUTARIO",
                perfil,
                "Assistente operacional fazenda/tributário do PJB",
                perfil.citizenLike()
                        ? "Vou te orientar no preenchimento sem mexer sozinho em juros, multas ou descontos sensíveis."
                        : "A assistência foi desenhada para reduzir retrabalho em memória fazendária, preservando rastreabilidade integral.",
                CalculatorHelpMessages.fazendaMessages(),
                List.copyOf(pendentes),
                List.copyOf(bloqueios),
                List.copyOf(ajustes),
                List.copyOf(proximosPassos),
                Map.copyOf(autopreenchimento),
                Map.copyOf(desenhoAssistido),
                CalculatorHelpMessages.iaGuardrails(),
                Instant.now()
        );
    }

    public CalculoJudicialAssistenciaResponse orientarCustas(CustasProcessuaisCalculoAvancadoRequest request, Authentication authentication) {
        CalculoJudicialSolicitantePerfil perfil = profileResolverService.resolve(authentication, request == null ? null : request.perfilSolicitante());
        List<String> pendentes = new ArrayList<>();
        List<String> bloqueios = new ArrayList<>();
        List<String> ajustes = new ArrayList<>();
        List<String> proximosPassos = new ArrayList<>();
        Map<String, Object> autopreenchimento = new LinkedHashMap<>();
        Map<String, Object> desenhoAssistido = desenhoBase("CUSTAS_PROCESSUAIS", perfil);

        if (request == null || request.valorCausa() == null) {
            pendentes.add("Informar o valor da causa ou base econômica.");
        }
        if (request != null && request.dataBaseCalculo() != null && request.dataFinalCalculo() != null && request.dataFinalCalculo().isBefore(request.dataBaseCalculo())) {
            bloqueios.add("A data final do cálculo não pode ser anterior à data-base.");
        }
        autopreenchimento.put("percentualTaxaJudiciariaSugerido", request != null && request.percentualTaxaJudiciaria() != null ? request.percentualTaxaJudiciaria() : new BigDecimal("0.015000"));
        autopreenchimento.put("percentualPreparoRecursalSugerido", request != null && request.percentualPreparoRecursal() != null ? request.percentualPreparoRecursal() : BigDecimal.ZERO.setScale(2));
        autopreenchimento.put("fatorAtualizacaoCustasSugerido", request != null && request.fatorAtualizacaoCustas() != null ? request.fatorAtualizacaoCustas() : BigDecimal.ZERO.setScale(2));
        if (request == null || request.tribunal() == null || request.tribunal().isBlank()) {
            pendentes.add("Informar o tribunal ou o portal de custas de referência.");
        }
        if (request == null || request.sistemaOrigem() == null || request.sistemaOrigem().isBlank()) {
            ajustes.add("A IA assistiva pode sugerir sistema de origem como e-SAJ, Projudi, eproc ou portal local de custas, mas o usuário deve confirmar.");
        }
        if (request != null && request.depositoJudicialVinculado() != null && request.depositoJudicialVinculado().signum() > 0) {
            ajustes.add("Depósito judicial informado será tratado como abatimento separado do total de custas.");
        }
        proximosPassos.add("Confirmar percentual da taxa judiciária e piso mínimo na tabela vigente do tribunal.");
        proximosPassos.add("Separar preparo, diligências, despesas postais, pesquisas conveniadas e porte em rubricas distintas.");
        proximosPassos.add("Verificar se há depósito judicial anterior que deva ser abatido do saldo projetado.");
        desenhoAssistido.put("rotaEntradaDireta", CalculoJudicialDomainSupport.apiRoutes("CUSTAS_PROCESSUAIS").get("workspace"));
        desenhoAssistido.put("rotaAjuda", CalculoJudicialDomainSupport.apiRoutes("CUSTAS_PROCESSUAIS").get("ajuda"));
        desenhoAssistido.put("rotaCalculo", CalculoJudicialDomainSupport.apiRoutes("CUSTAS_PROCESSUAIS").get("json"));
        desenhoAssistido.put("rotaPdf", CalculoJudicialDomainSupport.apiRoutes("CUSTAS_PROCESSUAIS").get("pdf"));
        desenhoAssistido.putAll(frontendContractService.frontendMeta("CUSTAS_PROCESSUAIS", perfil, "assistencia"));
        desenhoAssistido.put("frontendReady", Boolean.TRUE);
        desenhoAssistido.put("liveExperience", CalculatorHelpMessages.liveComponentDesign("CUSTAS_PROCESSUAIS", perfil));
        desenhoAssistido.put("quickStartJourney", CalculatorHelpMessages.quickStartJourney("CUSTAS_PROCESSUAIS", perfil));

        return new CalculoJudicialAssistenciaResponse(
                "CUSTAS_PROCESSUAIS",
                perfil,
                "Assistente operacional de custas e despesas do PJB",
                perfil.citizenLike()
                        ? "Vou te guiar na taxa, preparo, despesas e depósito judicial sem esconder rubricas nem somar valores de forma opaca."
                        : "A assistência foi desenhada para reduzir atrito em custas, guias e depósitos, preservando segregação auditável das rubricas.",
                CalculatorHelpMessages.custasMessages(),
                List.copyOf(pendentes),
                List.copyOf(bloqueios),
                List.copyOf(ajustes),
                List.copyOf(proximosPassos),
                Map.copyOf(autopreenchimento),
                Map.copyOf(desenhoAssistido),
                CalculatorHelpMessages.iaGuardrails(),
                Instant.now()
        );
    }

    public CalculoJudicialAssistenciaResponse orientarFederalPrevidenciario(FederalPrevidenciarioCjfCalculoAvancadoRequest request, Authentication authentication) {
        CalculoJudicialSolicitantePerfil perfil = profileResolverService.resolve(authentication, request == null ? null : request.perfilSolicitante());
        List<String> pendentes = new ArrayList<>();
        List<String> bloqueios = new ArrayList<>();
        List<String> ajustes = new ArrayList<>();
        List<String> proximosPassos = new ArrayList<>();
        Map<String, Object> autopreenchimento = new LinkedHashMap<>();
        Map<String, Object> desenhoAssistido = desenhoBase("FEDERAL_PREVIDENCIARIO_CJF", perfil);

        if (request == null || request.rendaMensalAtual() == null) {
            pendentes.add("Informar a renda mensal do benefício ou da diferença mensal.");
        }
        if (request == null || request.dib() == null) {
            pendentes.add("Informar a DIB.");
        }
        if (request == null || request.dataCalculo() == null) {
            pendentes.add("Informar a data do cálculo.");
        }
        if (request != null && request.dib() != null && request.dataCalculo() != null) {
            if (request.dataCalculo().isBefore(request.dib())) {
                bloqueios.add("A data do cálculo não pode ser anterior à DIB.");
            } else {
                long competencias = ChronoUnit.MONTHS.between(request.dib().withDayOfMonth(1), request.dataCalculo().withDayOfMonth(1)) + 1;
                autopreenchimento.put("competenciasVencidasEstimadas", competencias);
                ajustes.add("A IA assistiva consegue estimar as competências vencidas a partir de DIB e data do cálculo.");
            }
        }
        autopreenchimento.put("aplicarPrescricaoQuinquenalSugerido", request != null && request.dataAjuizamento() != null);
        autopreenchimento.put("incluirAbonoAnualSugerido", Boolean.TRUE);
        autopreenchimento.put("percentualJurosMoraMensalSugerido", request != null && request.percentualJurosMoraMensal() != null ? request.percentualJurosMoraMensal() : new BigDecimal("0.005000"));
        autopreenchimento.put("tetoRpvEmSalariosMinimosSugerido", request != null && request.tetoRpvEmSalariosMinimos() != null ? request.tetoRpvEmSalariosMinimos() : new BigDecimal("60"));
        if (request == null || request.dataCitacao() == null) {
            ajustes.add("A IA assistiva pode lembrar a data de citação como marco prudencial de juros, mas o usuário deve confirmar a premissa processual adotada.");
        }
        if (request == null || request.taxasCorrecaoMensais() == null || request.taxasCorrecaoMensais().isEmpty()) {
            proximosPassos.add("Inserir série oficial de correção quando a memória precisar reproduzir tabela institucional por competência.");
        }
        proximosPassos.add("Separar pagamentos administrativos e tutela em rubricas de abatimento distintas.");
        proximosPassos.add("Conferir se o resultado deve ser classificado como RPV ou precatório segundo o salário mínimo e o teto adotado.");
        desenhoAssistido.put("rotaEntradaDireta", CalculoJudicialDomainSupport.apiRoutes("FEDERAL_PREVIDENCIARIO_CJF").get("workspace"));
        desenhoAssistido.put("rotaAjuda", CalculoJudicialDomainSupport.apiRoutes("FEDERAL_PREVIDENCIARIO_CJF").get("ajuda"));
        desenhoAssistido.put("rotaCalculo", CalculoJudicialDomainSupport.apiRoutes("FEDERAL_PREVIDENCIARIO_CJF").get("json"));
        desenhoAssistido.put("rotaPdf", CalculoJudicialDomainSupport.apiRoutes("FEDERAL_PREVIDENCIARIO_CJF").get("pdf"));
        desenhoAssistido.putAll(frontendContractService.frontendMeta("FEDERAL_PREVIDENCIARIO_CJF", perfil, "assistencia"));
        desenhoAssistido.put("frontendReady", Boolean.TRUE);
        desenhoAssistido.put("liveExperience", CalculatorHelpMessages.liveComponentDesign("FEDERAL_PREVIDENCIARIO_CJF", perfil));
        desenhoAssistido.put("quickStartJourney", CalculatorHelpMessages.quickStartJourney("FEDERAL_PREVIDENCIARIO_CJF", perfil));

        return new CalculoJudicialAssistenciaResponse(
                "FEDERAL_PREVIDENCIARIO_CJF",
                perfil,
                "Assistente operacional federal/JEF previdenciário do PJB",
                perfil.citizenLike()
                        ? "Vou te conduzir na memória de atrasados previdenciários sem esconder competências, abatimentos ou classificação do pagamento."
                        : "A assistência foi desenhada para reduzir atrito em atrasados previdenciários federais, preservando trilha técnica e aderência institucional.",
                CalculatorHelpMessages.federalPrevidenciarioMessages(),
                List.copyOf(pendentes),
                List.copyOf(bloqueios),
                List.copyOf(ajustes),
                List.copyOf(proximosPassos),
                Map.copyOf(autopreenchimento),
                Map.copyOf(desenhoAssistido),
                CalculatorHelpMessages.iaGuardrails(),
                Instant.now()
        );
    }

    public Map<String, Object> metadataTrabalhista(TrabalhistaCalculoAvancadoRequest request, CalculoJudicialSolicitantePerfil perfil) {
        CalculoJudicialAssistenciaResponse assistencia = orientarTrabalhista(request, null);
        return metadataBase("TRABALHISTA_CLT", perfil, assistencia, CalculatorHelpMessages.trabalhistaMessages(), List.of("Dados iniciais", "Jornada e verbas", "Reflexos e FGTS", "Atualização", "Penalidades e encargos", "Observações"));
    }

    public Map<String, Object> metadataFazenda(FazendaTributarioCalculoAvancadoRequest request, CalculoJudicialSolicitantePerfil perfil) {
        CalculoJudicialAssistenciaResponse assistencia = orientarFazenda(request, null);
        return metadataBase("FAZENDA_TRIBUTARIO", perfil, assistencia, CalculatorHelpMessages.fazendaMessages(), List.of("Dados do processo", "Correção monetária", "Juros moratórios", "Multas e descontos", "Encargos e honorários", "Compensações e garantias"));
    }

    public Map<String, Object> metadataCustas(CustasProcessuaisCalculoAvancadoRequest request, CalculoJudicialSolicitantePerfil perfil) {
        CalculoJudicialAssistenciaResponse assistencia = orientarCustas(request, null);
        return metadataBase("CUSTAS_PROCESSUAIS", perfil, assistencia, CalculatorHelpMessages.custasMessages(), List.of("Dados básicos", "Taxa e preparo", "Despesas processuais", "Atualização", "Abatimentos e depósito", "Observações"));
    }

    public Map<String, Object> metadataFederalPrevidenciario(FederalPrevidenciarioCjfCalculoAvancadoRequest request, CalculoJudicialSolicitantePerfil perfil) {
        CalculoJudicialAssistenciaResponse assistencia = orientarFederalPrevidenciario(request, null);
        return metadataBase("FEDERAL_PREVIDENCIARIO_CJF", perfil, assistencia, CalculatorHelpMessages.federalPrevidenciarioMessages(), List.of("Dados do benefício", "Marco temporal", "Parcelas e abono", "Atualização e juros", "Abatimentos", "Classificação do pagamento", "Observações"));
    }

    private Map<String, Object> metadataBase(String dominio,
                                             CalculoJudicialSolicitantePerfil perfil,
                                             CalculoJudicialAssistenciaResponse assistencia,
                                             List<String> mensagensAjuda,
                                             List<String> secoes) {
        String dominioCanonico = CalculoJudicialDomainSupport.requireSupported(dominio);
        CalculoJudicialSolicitantePerfil perfilEfetivo = perfil == null ? CalculoJudicialSolicitantePerfil.CIDADAO : perfil;
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("landingTab", "Calculadora");
        metadata.put("workspaceDomain", dominioCanonico);
        metadata.put("workspaceRoute", CalculoJudicialDomainSupport.apiRoutes(dominioCanonico).get("workspace"));
        metadata.put("assistenteRoute", CalculoJudicialDomainSupport.apiRoutes(dominioCanonico).get("assistente"));
        metadata.put("apiRoutes", CalculoJudicialDomainSupport.apiRoutes(dominioCanonico));
        metadata.put("apiAliases", CalculoJudicialDomainSupport.aliases(dominioCanonico));
        metadata.put("officialBenchmarkCoverage", CalculatorHelpMessages.officialBenchmarkSignals());
        metadata.put("expansionIdeas", CalculatorHelpMessages.expansionIdeas());
        metadata.putAll(frontendContractService.frontendMeta(dominioCanonico, perfilEfetivo, "assistencia_metadata"));
        metadata.put("helpMessages", mensagensAjuda);
        metadata.put("dailyUseBehavior", CalculatorHelpMessages.dailyBehavior());
        metadata.put("workspaceSignals", CalculatorHelpMessages.smartWorkspaceSignals(perfilEfetivo));
        metadata.put("safeAutomationCapabilities", CalculatorHelpMessages.safeAutomationCapabilities());
        metadata.put("guardrailsIa", CalculatorHelpMessages.iaGuardrails());
        metadata.put("contractVersion", frontendContractService.version());
        metadata.put("contractFingerprint", frontendContractService.fingerprint());
        metadata.put("completionMessages", CalculatorHelpMessages.completionMessages(dominioCanonico, perfilEfetivo));
        metadata.put("readyNotificationTemplates", Map.of(
                "manual", CalculatorHelpMessages.readyNotificationTemplate(dominioCanonico, perfilEfetivo, false),
                "iaAssistida", CalculatorHelpMessages.readyNotificationTemplate(dominioCanonico, perfilEfetivo, true)
        ));
        metadata.put("completionExperience", Map.of(
                "manual", CalculatorHelpMessages.readyNotificationTemplate(dominioCanonico, perfilEfetivo, false),
                "iaAssistida", CalculatorHelpMessages.readyNotificationTemplate(dominioCanonico, perfilEfetivo, true)
        ));
        metadata.put("assistivePanel", Map.of(
                "titulo", assistencia.titulo(),
                "mensagemAbertura", assistencia.mensagemAbertura(),
                "camposCriticosPendentes", assistencia.camposCriticosPendentes(),
                "validacoesBloqueantes", assistencia.validacoesBloqueantes(),
                "ajustesAutomaticosSugeridos", assistencia.ajustesAutomaticosSugeridos(),
                "autopreenchimentoSeguro", assistencia.autopreenchimentoSeguro()
        ));
        metadata.put("navigationDesign", Map.of(
                "entryMode", "direct_tab_access",
                "stickySummary", Boolean.TRUE,
                "inlineHelp", Boolean.TRUE,
                "inlineValidation", Boolean.TRUE,
                "defaultProfileTone", perfilEfetivo.citizenLike() ? "guiado" : "auditavel",
                "sections", secoes,
                "liveExperience", CalculatorHelpMessages.liveComponentDesign(dominioCanonico, perfilEfetivo),
                "quickStartJourney", CalculatorHelpMessages.quickStartJourney(dominioCanonico, perfilEfetivo)
        ));
        metadata.put("progressModel", progressModel(dominioCanonico, assistencia));
        metadata.put("workflowState", workflowState(dominioCanonico, assistencia));
        metadata.put("financialIaRoute", CalculoJudicialDomainSupport.financialAiExecuteRoute());
        metadata.put("financialIaPresetDomain", dominioCanonico);
        metadata.put("financialAiExecutionModel", "planner_router_normalizer_validator_executor_verifier");
        metadata.put("aiAgents", frontendContractService.aiAgentsCatalog());
        metadata.put("financialIaMessages", CalculatorHelpMessages.financialIaMessages());
        return metadata;
    }

    private Map<String, Object> desenhoBase(String dominio, CalculoJudicialSolicitantePerfil perfil) {
        Map<String, Object> desenho = new LinkedHashMap<>();
        desenho.put("dominio", dominio);
        desenho.put("menu", "Calculadora");
        desenho.put("tom", perfil.citizenLike() ? "guiado" : "técnico");
        desenho.put("componentes", List.of("cards de entrada", "assistente lateral", "resumo financeiro fixo", "mensagens por seção", "ações rápidas JSON/PDF"));
        desenho.put("componentesVivos", List.of("toast", "banner superior", "card de conclusão", "badge IA assistida", "ações rápidas"));
        desenho.put("validacao", List.of("datas coerentes", "parâmetros obrigatórios", "campos incompatíveis", "percentuais sensíveis"));
        desenho.put("financialIaRoute", CalculoJudicialDomainSupport.financialAiExecuteRoute());
        desenho.put("financialIaPresetDomain", dominio);
        desenho.put("financialAiExecutionModel", "planner_router_normalizer_validator_executor_verifier");
        desenho.put("aiAgents", frontendContractService.aiAgentsCatalog());
        desenho.put("financialIaMessages", CalculatorHelpMessages.financialIaMessages());
        return desenho;
    }

    private List<Map<String, Object>> progressModel(String dominio, CalculoJudicialAssistenciaResponse assistencia) {
        return List.of(
                Map.of("codigo", "entrada", "titulo", "Entrada direta", "status", "READY"),
                Map.of("codigo", "parametros", "titulo", "Parâmetros críticos", "status", assistencia.camposCriticosPendentes().isEmpty() ? "READY" : "PENDING", "pendencias", assistencia.camposCriticosPendentes()),
                Map.of("codigo", "validacao", "titulo", "Validação", "status", assistencia.validacoesBloqueantes().isEmpty() ? "READY" : "BLOCKED", "bloqueios", assistencia.validacoesBloqueantes()),
                Map.of("codigo", "assistencia", "titulo", "IA assistiva", "status", "READY", "sugestoes", assistencia.ajustesAutomaticosSugeridos()),
                Map.of("codigo", "memoria", "titulo", "Memória final", "status", "READY"),
                Map.of("codigo", "pdf", "titulo", "PDF", "status", "READY"),
                Map.of("codigo", "dominio", "titulo", "Domínio ativo", "status", dominio)
        );
    }

    private Map<String, Object> workflowState(String dominio, CalculoJudicialAssistenciaResponse assistencia) {
        String status = !assistencia.validacoesBloqueantes().isEmpty()
                ? "BLOCKED"
                : !assistencia.camposCriticosPendentes().isEmpty()
                ? "PENDING"
                : "READY";
        return Map.of(
                "dominio", dominio,
                "status", status,
                "bloqueado", !assistencia.validacoesBloqueantes().isEmpty(),
                "pendencias", assistencia.camposCriticosPendentes(),
                "bloqueios", assistencia.validacoesBloqueantes(),
                "proximaAcao", status.equals("READY") ? "calcular_ou_gerar_pdf" : status.equals("BLOCKED") ? "corrigir_bloqueios" : "completar_parametros"
        );
    }
}
