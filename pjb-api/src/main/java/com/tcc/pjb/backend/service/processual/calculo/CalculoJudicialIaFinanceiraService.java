package com.tcc.pjb.backend.service.processual.calculo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialAssistenciaResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialIaFinanceiraResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialIaFinanceiraCommandRequest;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialResumoResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoIndiceMensalRequest;
import com.tcc.pjb.backend.model.dto.processual.calculo.CustasProcessuaisCalculoAvancadoRequest;
import com.tcc.pjb.backend.model.dto.processual.calculo.FazendaTributarioCalculoAvancadoRequest;
import com.tcc.pjb.backend.model.dto.processual.calculo.FederalPrevidenciarioCjfCalculoAvancadoRequest;
import com.tcc.pjb.backend.model.dto.processual.calculo.TrabalhistaCalculoAvancadoRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CalculoJudicialIaFinanceiraService {

    private static final String AGENTE = "IA_FINANCEIRA_PJB";

    private final CalculoJudicialAssistenciaService assistenciaService;
    private final CalculoJudicialFacadeService facadeService;
    private final CalculoJudicialFrontendContractService frontendContractService;
    private final CalculoJudicialEconomicReferenceService economicReferenceService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public CalculoJudicialIaFinanceiraService(CalculoJudicialAssistenciaService assistenciaService,
                                               CalculoJudicialFacadeService facadeService,
                                               CalculoJudicialFrontendContractService frontendContractService,
                                               CalculoJudicialEconomicReferenceService economicReferenceService,
                                               ObjectMapper objectMapper,
                                               Validator validator) {
        this.assistenciaService = Objects.requireNonNull(assistenciaService);
        this.facadeService = Objects.requireNonNull(facadeService);
        this.frontendContractService = Objects.requireNonNull(frontendContractService);
        this.economicReferenceService = Objects.requireNonNull(economicReferenceService);
        this.objectMapper = Objects.requireNonNull(objectMapper).copy().findAndRegisterModules();
        this.validator = Objects.requireNonNull(validator);
    }

    public CalculoJudicialIaFinanceiraResponse executar(CalculoJudicialIaFinanceiraCommandRequest command, Authentication authentication) {
        if (command == null) {
            throw new IllegalArgumentException("Comando da IA financeira obrigatório.");
        }
        String dominio = CalculoJudicialDomainSupport.requireSupported(command.dominio());
        Map<String, Object> payload = command.payload() == null ? Map.of() : command.payload();
        return switch (dominio) {
            case "TRABALHISTA_CLT" -> enrichGeneric(executarTrabalhista(convert(payload, TrabalhistaCalculoAvancadoRequest.class), authentication), command, dominio);
            case "FAZENDA_TRIBUTARIO" -> enrichGeneric(executarFazenda(convert(payload, FazendaTributarioCalculoAvancadoRequest.class), authentication), command, dominio);
            case "CUSTAS_PROCESSUAIS" -> enrichGeneric(executarCustas(convert(payload, CustasProcessuaisCalculoAvancadoRequest.class), authentication), command, dominio);
            case "FEDERAL_PREVIDENCIARIO_CJF" -> enrichGeneric(executarFederalPrevidenciario(convert(payload, FederalPrevidenciarioCjfCalculoAvancadoRequest.class), authentication), command, dominio);
            default -> throw new IllegalArgumentException("Domínio financeiro não suportado.");
        };
    }

    public CalculoJudicialIaFinanceiraResponse executarTrabalhista(TrabalhistaCalculoAvancadoRequest request, Authentication authentication) {
        CalculoJudicialAssistenciaResponse assistencia = assistenciaService.orientarTrabalhista(request, authentication);
        AutomationEnvelope<TrabalhistaCalculoAvancadoRequest> envelope = normalizeTrabalhista(request);
        List<String> pendencias = merge(assistencia.camposCriticosPendentes(), envelope.pendencias());
        List<String> bloqueios = merge(assistencia.validacoesBloqueantes(), envelope.bloqueios());
        if (!bloqueios.isEmpty()) {
            return response("TRABALHISTA_CLT", assistencia.perfilResolvido(), assistencia, envelope, pendencias, bloqueios, null, "BLOCKED", false,
                    "A IA financeira travou a execução automática porque encontrou bloqueios que precisam ser resolvidos antes da calculadora real ser acionada.");
        }
        if (!pendencias.isEmpty()) {
            return response("TRABALHISTA_CLT", assistencia.perfilResolvido(), assistencia, envelope, pendencias, bloqueios, null, "PENDING_INPUT", false,
                    "A IA financeira organizou a pré-análise, mas ainda faltam dados essenciais para chamar a calculadora real sem risco material.");
        }
        CalculoJudicialResumoResponse resultado = facadeService.calcularTrabalhista(envelope.request(), authentication);
        return response("TRABALHISTA_CLT", assistencia.perfilResolvido(), assistencia, envelope, pendencias, bloqueios, resultado, "READY", true,
                "A IA financeira preencheu o que era determinístico, chamou a calculadora real trabalhista e devolveu a memória auditável sem sair da trilha oficial.");
    }

    public CalculoJudicialIaFinanceiraResponse executarFazenda(FazendaTributarioCalculoAvancadoRequest request, Authentication authentication) {
        CalculoJudicialAssistenciaResponse assistencia = assistenciaService.orientarFazenda(request, authentication);
        AutomationEnvelope<FazendaTributarioCalculoAvancadoRequest> envelope = normalizeFazenda(request);
        List<String> pendencias = merge(assistencia.camposCriticosPendentes(), envelope.pendencias());
        List<String> bloqueios = merge(assistencia.validacoesBloqueantes(), envelope.bloqueios());
        if (!bloqueios.isEmpty()) {
            return response("FAZENDA_TRIBUTARIO", assistencia.perfilResolvido(), assistencia, envelope, pendencias, bloqueios, null, "BLOCKED", false,
                    "A IA financeira identificou bloqueios e evitou rodar a calculadora fazendária com premissas defeituosas.");
        }
        if (!pendencias.isEmpty()) {
            return response("FAZENDA_TRIBUTARIO", assistencia.perfilResolvido(), assistencia, envelope, pendencias, bloqueios, null, "PENDING_INPUT", false,
                    "A IA financeira preparou a memória fazendária, mas ainda depende de dados mínimos para disparar a calculadora real com segurança.");
        }
        CalculoJudicialResumoResponse resultado = facadeService.calcularFazenda(envelope.request(), authentication);
        return response("FAZENDA_TRIBUTARIO", assistencia.perfilResolvido(), assistencia, envelope, pendencias, bloqueios, resultado, "READY", true,
                "A IA financeira consolidou parâmetros determinísticos e acionou a calculadora real fazendária, preservando juros, mora e trilha auditável.");
    }

    public CalculoJudicialIaFinanceiraResponse executarCustas(CustasProcessuaisCalculoAvancadoRequest request, Authentication authentication) {
        CalculoJudicialAssistenciaResponse assistencia = assistenciaService.orientarCustas(request, authentication);
        AutomationEnvelope<CustasProcessuaisCalculoAvancadoRequest> envelope = normalizeCustas(request);
        List<String> pendencias = merge(assistencia.camposCriticosPendentes(), envelope.pendencias());
        List<String> bloqueios = merge(assistencia.validacoesBloqueantes(), envelope.bloqueios());
        if (!bloqueios.isEmpty()) {
            return response("CUSTAS_PROCESSUAIS", assistencia.perfilResolvido(), assistencia, envelope, pendencias, bloqueios, null, "BLOCKED", false,
                    "A IA financeira evitou disparar a calculadora de custas porque há conflito em datas ou parâmetros estruturais da guia.");
        }
        if (!pendencias.isEmpty()) {
            return response("CUSTAS_PROCESSUAIS", assistencia.perfilResolvido(), assistencia, envelope, pendencias, bloqueios, null, "PENDING_INPUT", false,
                    "A IA financeira organizou taxa, preparo e rubricas, mas ainda faltam dados mínimos para executar a calculadora real de custas sem risco.");
        }
        CalculoJudicialResumoResponse resultado = facadeService.calcularCustas(envelope.request(), authentication);
        return response("CUSTAS_PROCESSUAIS", assistencia.perfilResolvido(), assistencia, envelope, pendencias, bloqueios, resultado, "READY", true,
                "A IA financeira separou custas, despesas e abatimentos e chamou a calculadora real de custas para produzir a memória final auditável.");
    }

    public CalculoJudicialIaFinanceiraResponse executarFederalPrevidenciario(FederalPrevidenciarioCjfCalculoAvancadoRequest request, Authentication authentication) {
        CalculoJudicialAssistenciaResponse assistencia = assistenciaService.orientarFederalPrevidenciario(request, authentication);
        AutomationEnvelope<FederalPrevidenciarioCjfCalculoAvancadoRequest> envelope = normalizeFederalPrevidenciario(request);
        List<String> pendencias = merge(assistencia.camposCriticosPendentes(), envelope.pendencias());
        List<String> bloqueios = merge(assistencia.validacoesBloqueantes(), envelope.bloqueios());
        if (!bloqueios.isEmpty()) {
            return response("FEDERAL_PREVIDENCIARIO_CJF", assistencia.perfilResolvido(), assistencia, envelope, pendencias, bloqueios, null, "BLOCKED", false,
                    "A IA financeira travou a execução automática previdenciária para impedir cálculo de atrasados com marco temporal incoerente.");
        }
        if (!pendencias.isEmpty()) {
            return response("FEDERAL_PREVIDENCIARIO_CJF", assistencia.perfilResolvido(), assistencia, envelope, pendencias, bloqueios, null, "PENDING_INPUT", false,
                    "A IA financeira estruturou os atrasados previdenciários, mas ainda depende de dados mínimos para chamar a calculadora real sem erro material.");
        }
        CalculoJudicialResumoResponse resultado = facadeService.calcularFederalPrevidenciario(envelope.request(), authentication);
        return response("FEDERAL_PREVIDENCIARIO_CJF", assistencia.perfilResolvido(), assistencia, envelope, pendencias, bloqueios, resultado, "READY", true,
                "A IA financeira consolidou os parâmetros federais/JEF e acionou a calculadora real previdenciária, preservando correção, juros, abatimentos e classificação do pagamento.");
    }

    private CalculoJudicialIaFinanceiraResponse response(String dominio,
                                                         CalculoJudicialSolicitantePerfil perfil,
                                                         CalculoJudicialAssistenciaResponse assistencia,
                                                         AutomationEnvelope<?> envelope,
                                                         List<String> pendencias,
                                                         List<String> bloqueios,
                                                         CalculoJudicialResumoResponse resultado,
                                                         String status,
                                                         boolean calculoExecutado,
                                                         String mensagemResultado) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("agentCode", AGENTE);
        metadata.put("executionMode", calculoExecutado ? "executado_pela_calculadora_real" : "pre_validacao_sem_execucao");
        metadata.put("sourceOfTruth", calculoExecutado ? "CALCULADORA_REAL" : "IA_FINANCEIRA_PRECHECK");
        metadata.put("calculatorRoute", CalculoJudicialDomainSupport.jsonRoute(dominio));
        metadata.put("calculatorPdfRoute", CalculoJudicialDomainSupport.pdfRoute(dominio));
        metadata.put("officialTablesRoute", CalculoJudicialDomainSupport.officialTablesRoute(dominio));
        metadata.put("economicReferencesRoute", CalculoJudicialDomainSupport.economicReferencesRoute());
        metadata.put("financialAiLiveAjuizamentoRoute", CalculoJudicialDomainSupport.financialAiLiveFilingRoute());
        metadata.put("financialAiRoute", CalculoJudicialDomainSupport.financialAiRoute(dominio));
        metadata.put("routePolicy", CalculoJudicialDomainSupport.routePolicy(dominio));
        metadata.put("apiContract", frontendContractService.apiContract(dominio));
        metadata.put("profileCapabilities", frontendContractService.profileCapabilities(perfil));
        metadata.put("safeAutomationCapabilities", CalculatorHelpMessages.safeAutomationCapabilities());
        metadata.put("methods2026", CalculatorHelpMessages.financialIa2026Methods());
        metadata.put("economicReferences", economicReferenceService.panelSnapshot());
        metadata.put("financialKnowledgeBase", frontendContractService.financialKnowledgeBase());
        metadata.put("financialAiPanel", frontendContractService.financialAiPanel());
        metadata.put("executionModel", "planner_router_normalizer_validator_executor_verifier");
        metadata.put("schemaDiscipline", "strict_payload_to_typed_request");
        metadata.put("verificationMode", "post_execution_consistency_gate");
        metadata.put("officialBenchmarkCoverage", CalculatorHelpMessages.officialBenchmarkSignals());
        metadata.put("guardrails", CalculatorHelpMessages.iaGuardrails());
        metadata.put("autopreenchimentoAplicado", envelope.autopreenchimento());
        metadata.put("camposConfirmacaoRecomendada", envelope.confirmacoes());
        metadata.put("autoExecutionEligible", bloqueios.isEmpty() && pendencias.isEmpty());
        metadata.put("frontendMeta", frontendContractService.frontendMeta(dominio, perfil, "ia_financeira"));
        if (resultado != null) {
            metadata.put("resultadoMetadata", resultado.metadata());
        }
        return new CalculoJudicialIaFinanceiraResponse(
                AGENTE,
                dominio,
                perfil,
                status,
                calculoExecutado,
                assistencia.mensagemAbertura(),
                mensagemResultado,
                CalculatorHelpMessages.iaGuardrails(),
                List.copyOf(pendencias),
                List.copyOf(bloqueios),
                List.copyOf(merge(assistencia.ajustesAutomaticosSugeridos(), envelope.ajustes())),
                List.copyOf(envelope.confirmacoes()),
                Map.copyOf(envelope.autopreenchimento()),
                safeMetadata(metadata),
                assistencia,
                resultado,
                Instant.now()
        );
    }

    private CalculoJudicialIaFinanceiraResponse enrichGeneric(CalculoJudicialIaFinanceiraResponse base,
                                                             CalculoJudicialIaFinanceiraCommandRequest command,
                                                             String dominio) {
        Map<String, Object> metadata = new LinkedHashMap<>(base.metadata());
        metadata.put("entryRoute", CalculoJudicialDomainSupport.financialAiExecuteRoute());
        metadata.put("executionProfile", command.executionProfile() == null || command.executionProfile().isBlank() ? "default_2026" : command.executionProfile().trim());
        if (command.pedidoUsuario() != null && !command.pedidoUsuario().isBlank()) {
            metadata.put("pedidoUsuario", command.pedidoUsuario().trim());
        }
        metadata.put("payloadKeys", command.payload() == null ? List.of() : List.copyOf(command.payload().keySet()));
        metadata.put("routingConfidence", routingConfidence(dominio, command.payload()));
        metadata.put("schemaValidation", validatePayload(command.payload(), dominio));
        metadata.put("toolchain", List.of("router", "typed_converter", "validator", "assistencia", "calculator_real", "verifier"));
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return new CalculoJudicialIaFinanceiraResponse(
                base.agente(),
                base.dominio(),
                base.perfilResolvido(),
                base.status(),
                base.calculoExecutado(),
                base.mensagemAbertura(),
                base.mensagemResultado(),
                base.guardrails(),
                base.pendencias(),
                base.bloqueios(),
                base.ajustesAplicados(),
                base.confirmacoesRecomendadas(),
                base.autopreenchimentoAplicado(),
                safeMetadata(metadata),
                base.assistencia(),
                base.resultado(),
                base.geradoEm()
        );
    }

    private Map<String, Object> validatePayload(Map<String, Object> payload, String dominio) {
        Object typed = switch (dominio) {
            case "TRABALHISTA_CLT" -> convert(payload, TrabalhistaCalculoAvancadoRequest.class);
            case "FAZENDA_TRIBUTARIO" -> convert(payload, FazendaTributarioCalculoAvancadoRequest.class);
            case "CUSTAS_PROCESSUAIS" -> convert(payload, CustasProcessuaisCalculoAvancadoRequest.class);
            case "FEDERAL_PREVIDENCIARIO_CJF" -> convert(payload, FederalPrevidenciarioCjfCalculoAvancadoRequest.class);
            default -> null;
        };
        if (typed == null) {
            return Map.of("valid", false, "violations", List.of("payload_ausente"));
        }
        Set<ConstraintViolation<Object>> violations = validator.validate(typed);
        List<String> labels = violations.stream().map(v -> v.getPropertyPath() + ": " + v.getMessage()).sorted().toList();
        return Map.of(
                "valid", labels.isEmpty(),
                "violationCount", labels.size(),
                "violations", labels
        );
    }

    private <T> T convert(Map<String, Object> payload, Class<T> type) {
        Map<String, Object> safePayload = payload == null ? Map.of() : payload;
        return objectMapper.convertValue(safePayload, type);
    }

    private BigDecimal routingConfidence(String dominio, Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return new BigDecimal("0.55");
        }
        int keyCount = payload.size();
        BigDecimal base = switch (dominio) {
            case "TRABALHISTA_CLT" -> new BigDecimal(keyCount >= 3 ? "0.94" : "0.82");
            case "FAZENDA_TRIBUTARIO" -> new BigDecimal(keyCount >= 3 ? "0.93" : "0.80");
            case "CUSTAS_PROCESSUAIS" -> new BigDecimal(keyCount >= 2 ? "0.91" : "0.78");
            case "FEDERAL_PREVIDENCIARIO_CJF" -> new BigDecimal(keyCount >= 4 ? "0.95" : "0.81");
            default -> new BigDecimal("0.70");
        };
        return base;
    }

    private AutomationEnvelope<TrabalhistaCalculoAvancadoRequest> normalizeTrabalhista(TrabalhistaCalculoAvancadoRequest request) {
        List<String> pendencias = new ArrayList<>();
        List<String> bloqueios = new ArrayList<>();
        List<String> ajustes = new ArrayList<>();
        List<String> confirmacoes = new ArrayList<>();
        Map<String, Object> autopreenchimento = new LinkedHashMap<>();
        if (request == null) {
            pendencias.add("Informar o corpo base do pedido trabalhista para a IA financeira acionar a calculadora real.");
            return new AutomationEnvelope<>(null, autopreenchimento, pendencias, bloqueios, ajustes, confirmacoes);
        }
        if (request.salarioBase() == null) {
            pendencias.add("Informar salário-base.");
        }
        if (request.admissao() == null) {
            pendencias.add("Informar data de admissão.");
        }
        if (request.demissao() == null) {
            pendencias.add("Informar data de demissão.");
        }
        if (request.admissao() != null && request.demissao() != null && request.demissao().isBefore(request.admissao())) {
            bloqueios.add("A data de demissão não pode ser anterior à admissão.");
        }
        Integer diasTrabalhados = request.diasTrabalhadosNoMesRescisao();
        if (diasTrabalhados == null && request.demissao() != null) {
            diasTrabalhados = Math.min(30, request.demissao().getDayOfMonth());
            autopreenchimento.put("diasTrabalhadosNoMesRescisao", diasTrabalhados);
            ajustes.add("A IA financeira preencheu os dias trabalhados no mês rescisório a partir da data de demissão.");
        }
        BigDecimal cargaHoraria = defaultDecimal(request.cargaHorariaMensalBase(), new BigDecimal("220"), autopreenchimento, ajustes, "cargaHorariaMensalBase", "A IA financeira aplicou a jornada mensal padrão de 220 horas.");
        BigDecimal extras50 = zeroDecimal(request.quantidadeHorasExtras50(), autopreenchimento, "quantidadeHorasExtras50");
        BigDecimal extras100 = zeroDecimal(request.quantidadeHorasExtras100(), autopreenchimento, "quantidadeHorasExtras100");
        BigDecimal horasNoturnas = zeroDecimal(request.quantidadeHorasNoturnas(), autopreenchimento, "quantidadeHorasNoturnas");
        BigDecimal adicionalNoturno = request.percentualAdicionalNoturno();
        if (adicionalNoturno == null && horasNoturnas.signum() > 0) {
            adicionalNoturno = new BigDecimal("0.20");
            autopreenchimento.put("percentualAdicionalNoturno", adicionalNoturno);
            ajustes.add("A IA financeira aplicou 20% como adicional noturno prudencial para permitir a execução segura da calculadora.");
            confirmacoes.add("Confirmar se o adicional noturno aplicável é realmente de 20% ou se existe regime diverso no caso concreto.");
        }
        BigDecimal intervalo = zeroDecimal(request.quantidadeHorasIntervaloIntrajornada(), autopreenchimento, "quantidadeHorasIntervaloIntrajornada");
        BigDecimal outrasParcelas = zeroDecimal(request.outrasParcelasFixasMensais(), autopreenchimento, "outrasParcelasFixasMensais");
        Integer diasUteis = defaultInteger(request.diasUteisMediaMes(), 22, autopreenchimento, ajustes, "diasUteisMediaMes", "A IA financeira aplicou 22 dias úteis como média prudencial mensal.");
        Integer domingos = defaultInteger(request.domingosFeriadosMediaMes(), 8, autopreenchimento, ajustes, "domingosFeriadosMediaMes", "A IA financeira aplicou 8 domingos/feriados como média prudencial mensal.");
        Boolean incluirSaldo = defaultBoolean(request.incluirSaldoSalario(), Boolean.TRUE, autopreenchimento, "incluirSaldoSalario");
        Boolean incluirDecimo = defaultBoolean(request.incluirDecimoTerceiro(), Boolean.TRUE, autopreenchimento, "incluirDecimoTerceiro");
        Boolean incluirFerias = defaultBoolean(request.incluirFeriasProporcionais(), Boolean.TRUE, autopreenchimento, "incluirFeriasProporcionais");
        Boolean incluirAviso = defaultBoolean(request.incluirAvisoPrevio(), Boolean.TRUE, autopreenchimento, "incluirAvisoPrevio");
        Boolean incluirFgts = defaultBoolean(request.incluirFgtsMensal(), Boolean.TRUE, autopreenchimento, "incluirFgtsMensal");
        Boolean incluirReflexos = defaultBoolean(request.incluirReflexosEmFeriasDecimoTerceiroFgts(), Boolean.TRUE, autopreenchimento, "incluirReflexosEmFeriasDecimoTerceiroFgts");
        Boolean incluirInss = defaultBoolean(request.incluirInssSegurado(), Boolean.FALSE, autopreenchimento, "incluirInssSegurado");
        Boolean incluirIrrf = defaultBoolean(request.incluirIrrf(), Boolean.FALSE, autopreenchimento, "incluirIrrf");
        Boolean multa467 = defaultBoolean(request.aplicarMultaArt467(), Boolean.FALSE, autopreenchimento, "aplicarMultaArt467");
        Boolean multa477 = defaultBoolean(request.aplicarMultaArt477(), Boolean.FALSE, autopreenchimento, "aplicarMultaArt477");
        BigDecimal percentualPericulosidade = zeroDecimal(request.percentualPericulosidade(), autopreenchimento, "percentualPericulosidade");
        BigDecimal fatorPreJudicial = zeroDecimal(request.fatorPreJudicialIpcae(), autopreenchimento, "fatorPreJudicialIpcae");
        BigDecimal percentualInss = zeroDecimal(request.percentualInssSegurado(), autopreenchimento, "percentualInssSegurado");
        BigDecimal percentualIrrf = zeroDecimal(request.percentualIrrfEfetivo(), autopreenchimento, "percentualIrrfEfetivo");
        BigDecimal honorarios = zeroDecimal(request.percentualHonorariosSucumbenciais(), autopreenchimento, "percentualHonorariosSucumbenciais");
        Boolean multaFgts40 = request.incluirMultaFgts40();
        if (multaFgts40 == null) {
            boolean enable = request.tipoDispensa() != null && request.tipoDispensa().toUpperCase().contains("SEM_JUSTA_CAUSA");
            multaFgts40 = enable;
            autopreenchimento.put("incluirMultaFgts40", enable);
            if (enable) {
                ajustes.add("A IA financeira habilitou a multa de 40% do FGTS porque o tipo de dispensa informado indica dispensa sem justa causa.");
            }
        }
        TrabalhistaCalculoAvancadoRequest normalized = new TrabalhistaCalculoAvancadoRequest(
                request.tituloCalculo(),
                request.numeroProcesso(),
                request.reclamanteNome(),
                request.reclamadoNome(),
                request.perfilSolicitante(),
                request.nomeSolicitante(),
                request.registroProfissionalSolicitante(),
                request.salarioBase(),
                request.remuneracaoMedia(),
                request.admissao(),
                request.demissao(),
                diasTrabalhados,
                request.tipoDispensa(),
                cargaHoraria,
                request.valorHoraBaseInformado(),
                extras50,
                extras100,
                horasNoturnas,
                adicionalNoturno,
                request.grauInsalubridade(),
                request.baseInsalubridade(),
                percentualPericulosidade,
                outrasParcelas,
                diasUteis,
                domingos,
                incluirSaldo,
                incluirDecimo,
                incluirFerias,
                incluirAviso,
                incluirFgts,
                multaFgts40,
                request.dataInicioAtualizacao(),
                request.dataFimAtualizacao(),
                fatorPreJudicial,
                emptySeries(request.taxasSelicMensais()),
                emptyList(request.parcelasLivres()),
                intervalo,
                incluirReflexos,
                incluirInss,
                percentualInss,
                incluirIrrf,
                percentualIrrf,
                multa467,
                multa477,
                honorarios,
                request.diasAvisoPrevioInformado(),
                defaultString(request.criterioAtualizacaoNome(), "IPCA-E pré-judicial + SELIC judicial parametrizada", autopreenchimento, "criterioAtualizacaoNome"),
                defaultString(request.criterioJurosNome(), "SELIC judicial parametrizada", autopreenchimento, "criterioJurosNome"),
                request.observacoesTecnicas()
        );
        return new AutomationEnvelope<>(normalized, autopreenchimento, pendencias, bloqueios, ajustes, confirmacoes);
    }

    private AutomationEnvelope<FazendaTributarioCalculoAvancadoRequest> normalizeFazenda(FazendaTributarioCalculoAvancadoRequest request) {
        List<String> pendencias = new ArrayList<>();
        List<String> bloqueios = new ArrayList<>();
        List<String> ajustes = new ArrayList<>();
        List<String> confirmacoes = new ArrayList<>();
        Map<String, Object> autopreenchimento = new LinkedHashMap<>();
        if (request == null) {
            pendencias.add("Informar o corpo base do pedido fazendário para a IA financeira acionar a calculadora real.");
            return new AutomationEnvelope<>(null, autopreenchimento, pendencias, bloqueios, ajustes, confirmacoes);
        }
        if (request.principal() == null) {
            pendencias.add("Informar o valor principal.");
        }
        if (request.vencimento() == null) {
            pendencias.add("Informar a data de vencimento.");
        }
        if (request.dataCalculo() == null) {
            pendencias.add("Informar a data do cálculo.");
        }
        if (request.vencimento() != null && request.dataCalculo() != null && request.dataCalculo().isBefore(request.vencimento())) {
            bloqueios.add("A data do cálculo não pode ser anterior ao vencimento na memória fazendária.");
        }
        BigDecimal multaDiaria = defaultDecimal(request.percentualMultaMoraDiaria(), new BigDecimal("0.0033"), autopreenchimento, ajustes, "percentualMultaMoraDiaria", "A IA financeira aplicou a multa de mora diária padrão de 0,33%.");
        BigDecimal tetoMora = defaultDecimal(request.limitePercentualMultaMora(), new BigDecimal("0.20"), autopreenchimento, ajustes, "limitePercentualMultaMora", "A IA financeira aplicou o teto prudencial de 20% para a multa de mora.");
        BigDecimal multaOficio = zeroDecimal(request.percentualMultaOficio(), autopreenchimento, "percentualMultaOficio");
        BigDecimal encargo = zeroDecimal(request.percentualEncargoLegal(), autopreenchimento, "percentualEncargoLegal");
        BigDecimal honorarios = zeroDecimal(request.percentualHonorarios(), autopreenchimento, "percentualHonorarios");
        BigDecimal custas = zeroDecimal(request.custas(), autopreenchimento, "custas");
        Boolean maisUm = defaultBoolean(request.aplicarMaisUmPorCentoNoMesPagamento(), Boolean.TRUE, autopreenchimento, "aplicarMaisUmPorCentoNoMesPagamento");
        BigDecimal valorGarantido = zeroDecimal(request.valorGarantidoOuDepositado(), autopreenchimento, "valorGarantidoOuDepositado");
        BigDecimal reducao = zeroDecimal(request.percentualReducaoMulta(), autopreenchimento, "percentualReducaoMulta");
        BigDecimal descontoPrograma = zeroDecimal(request.percentualDescontoPrograma(), autopreenchimento, "percentualDescontoPrograma");
        Boolean proRata = defaultBoolean(request.aplicarProRataDie(), Boolean.FALSE, autopreenchimento, "aplicarProRataDie");
        if (reducao.signum() > 0 || descontoPrograma.signum() > 0) {
            confirmacoes.add("Confirmar a base normativa da redução de multa ou do desconto de programa antes de protocolar a memória fazendária.");
        }
        FazendaTributarioCalculoAvancadoRequest normalized = new FazendaTributarioCalculoAvancadoRequest(
                request.tituloCalculo(),
                request.numeroProcesso(),
                request.enteTributante(),
                request.tributo(),
                request.perfilSolicitante(),
                request.nomeSolicitante(),
                request.registroProfissionalSolicitante(),
                request.principal(),
                request.vencimento(),
                request.dataCalculo(),
                multaDiaria,
                tetoMora,
                multaOficio,
                encargo,
                honorarios,
                custas,
                maisUm,
                emptySeries(request.taxasSelicMensais()),
                emptyList(request.creditosCompensaveis()),
                defaultString(request.criterioCorrecaoMonetariaNome(), "SELIC acumulada parametrizada", autopreenchimento, "criterioCorrecaoMonetariaNome"),
                defaultString(request.criterioJurosNome(), "Juros parametrizados sobre tributo federal", autopreenchimento, "criterioJurosNome"),
                request.dataInicioJurosMora(),
                valorGarantido,
                reducao,
                descontoPrograma,
                proRata,
                request.observacoesTecnicas()
        );
        return new AutomationEnvelope<>(normalized, autopreenchimento, pendencias, bloqueios, ajustes, confirmacoes);
    }

    private AutomationEnvelope<CustasProcessuaisCalculoAvancadoRequest> normalizeCustas(CustasProcessuaisCalculoAvancadoRequest request) {
        List<String> pendencias = new ArrayList<>();
        List<String> bloqueios = new ArrayList<>();
        List<String> ajustes = new ArrayList<>();
        List<String> confirmacoes = new ArrayList<>();
        Map<String, Object> autopreenchimento = new LinkedHashMap<>();
        if (request == null) {
            pendencias.add("Informar o corpo base das custas para a IA financeira acionar a calculadora real.");
            return new AutomationEnvelope<>(null, autopreenchimento, pendencias, bloqueios, ajustes, confirmacoes);
        }
        if (request.valorCausa() == null) {
            pendencias.add("Informar o valor da causa.");
        }
        if (request.dataBaseCalculo() != null && request.dataFinalCalculo() != null && request.dataFinalCalculo().isBefore(request.dataBaseCalculo())) {
            bloqueios.add("A data final do cálculo não pode ser anterior à data-base das custas.");
        }
        BigDecimal percentualTaxa = defaultDecimal(request.percentualTaxaJudiciaria(), new BigDecimal("0.015"), autopreenchimento, ajustes, "percentualTaxaJudiciaria", "A IA financeira aplicou 1,5% como taxa judiciária prudencial até confirmação da tabela local.");
        BigDecimal valorMinimoTaxa = zeroDecimal(request.valorMinimoTaxaJudiciaria(), autopreenchimento, "valorMinimoTaxaJudiciaria");
        BigDecimal preparo = zeroDecimal(request.percentualPreparoRecursal(), autopreenchimento, "percentualPreparoRecursal");
        BigDecimal despesasPostais = zeroDecimal(request.despesasPostais(), autopreenchimento, "despesasPostais");
        BigDecimal diligencias = zeroDecimal(request.diligenciasOficialJustica(), autopreenchimento, "diligenciasOficialJustica");
        BigDecimal editais = zeroDecimal(request.despesasEditais(), autopreenchimento, "despesasEditais");
        BigDecimal pesquisas = zeroDecimal(request.pesquisasConveniadas(), autopreenchimento, "pesquisasConveniadas");
        BigDecimal porte = zeroDecimal(request.porteRemessaRetorno(), autopreenchimento, "porteRemessaRetorno");
        BigDecimal custasFinais = zeroDecimal(request.custasFinaisComplementares(), autopreenchimento, "custasFinaisComplementares");
        BigDecimal deposito = zeroDecimal(request.depositoJudicialVinculado(), autopreenchimento, "depositoJudicialVinculado");
        BigDecimal fatorAtualizacao = zeroDecimal(request.fatorAtualizacaoCustas(), autopreenchimento, "fatorAtualizacaoCustas");
        if (request.tribunal() == null || request.tribunal().isBlank()) {
            confirmacoes.add("Confirmar o tribunal e a tabela local de custas antes de usar o percentual prudencial sugerido pela IA financeira.");
        }
        CustasProcessuaisCalculoAvancadoRequest normalized = new CustasProcessuaisCalculoAvancadoRequest(
                request.tituloCalculo(),
                request.numeroProcesso(),
                request.tribunal(),
                request.sistemaOrigem(),
                request.classeProcessual(),
                request.perfilSolicitante(),
                request.nomeSolicitante(),
                request.registroProfissionalSolicitante(),
                request.valorCausa(),
                percentualTaxa,
                valorMinimoTaxa,
                preparo,
                despesasPostais,
                diligencias,
                editais,
                pesquisas,
                porte,
                custasFinais,
                deposito,
                fatorAtualizacao,
                request.dataBaseCalculo(),
                request.dataFinalCalculo(),
                request.unidadeReferenciaNome(),
                request.valorUnidadeReferencia(),
                request.observacoesTecnicas()
        );
        return new AutomationEnvelope<>(normalized, autopreenchimento, pendencias, bloqueios, ajustes, confirmacoes);
    }

    private AutomationEnvelope<FederalPrevidenciarioCjfCalculoAvancadoRequest> normalizeFederalPrevidenciario(FederalPrevidenciarioCjfCalculoAvancadoRequest request) {
        List<String> pendencias = new ArrayList<>();
        List<String> bloqueios = new ArrayList<>();
        List<String> ajustes = new ArrayList<>();
        List<String> confirmacoes = new ArrayList<>();
        Map<String, Object> autopreenchimento = new LinkedHashMap<>();
        if (request == null) {
            pendencias.add("Informar o corpo base do cálculo federal/JEF para a IA financeira acionar a calculadora real.");
            return new AutomationEnvelope<>(null, autopreenchimento, pendencias, bloqueios, ajustes, confirmacoes);
        }
        if (request.rendaMensalAtual() == null) {
            pendencias.add("Informar a renda mensal do benefício ou da diferença.");
        }
        if (request.dib() == null) {
            pendencias.add("Informar a DIB.");
        }
        if (request.dataCalculo() == null) {
            pendencias.add("Informar a data do cálculo.");
        }
        if (request.dib() != null && request.dataCalculo() != null && request.dataCalculo().isBefore(request.dib())) {
            bloqueios.add("A data do cálculo não pode ser anterior à DIB.");
        }
        Boolean prescricao = defaultBoolean(request.aplicarPrescricaoQuinquenal(), request.dataAjuizamento() != null, autopreenchimento, "aplicarPrescricaoQuinquenal");
        Boolean abono = defaultBoolean(request.incluirAbonoAnual(), Boolean.TRUE, autopreenchimento, "incluirAbonoAnual");
        BigDecimal pagoAdmin = zeroDecimal(request.parcelasPagasAdministrativamente(), autopreenchimento, "parcelasPagasAdministrativamente");
        BigDecimal pagoTutela = zeroDecimal(request.parcelasPagasPorTutela(), autopreenchimento, "parcelasPagasPorTutela");
        BigDecimal fatorCorrecao = zeroDecimal(request.fatorCorrecaoMonetaria(), autopreenchimento, "fatorCorrecaoMonetaria");
        BigDecimal juros = defaultDecimal(request.percentualJurosMoraMensal(), new BigDecimal("0.005000"), autopreenchimento, ajustes, "percentualJurosMoraMensal", "A IA financeira aplicou juros mensais prudenciais de 0,5% até confirmação do critério do caso.");
        BigDecimal honorarios = zeroDecimal(request.percentualHonorarios(), autopreenchimento, "percentualHonorarios");
        BigDecimal tetoRpv = defaultDecimal(request.tetoRpvEmSalariosMinimos(), new BigDecimal("60"), autopreenchimento, ajustes, "tetoRpvEmSalariosMinimos", "A IA financeira aplicou 60 salários mínimos como teto prudencial de RPV.");
        BigDecimal salarioMinimoReferencia = request.salarioMinimoReferencia();
        if (salarioMinimoReferencia == null) {
            salarioMinimoReferencia = toBigDecimal(economicReferenceService.panelSnapshot().get("salarioMinimoVigente"));
            if (salarioMinimoReferencia != null && salarioMinimoReferencia.signum() > 0) {
                autopreenchimento.put("salarioMinimoReferencia", salarioMinimoReferencia);
                ajustes.add("A IA financeira aplicou o salário mínimo nacional vigente do PJB como referência previdenciária inicial.");
            }
            confirmacoes.add("Confirmar o salário mínimo de referência antes de tratar a classificação do pagamento como RPV ou precatório em memória final.");
        }
        FederalPrevidenciarioCjfCalculoAvancadoRequest normalized = new FederalPrevidenciarioCjfCalculoAvancadoRequest(
                request.tituloCalculo(),
                request.numeroProcesso(),
                request.tribunal(),
                request.sistemaOrigem(),
                request.tipoBeneficio(),
                request.perfilSolicitante(),
                request.nomeSolicitante(),
                request.registroProfissionalSolicitante(),
                request.rendaMensalAtual(),
                request.dib(),
                request.dip(),
                request.dcb(),
                request.dataAjuizamento(),
                request.dataCitacao(),
                request.dataCalculo(),
                prescricao,
                abono,
                pagoAdmin,
                pagoTutela,
                emptySeries(request.taxasCorrecaoMensais()),
                fatorCorrecao,
                juros,
                honorarios,
                salarioMinimoReferencia,
                tetoRpv,
                defaultString(request.criterioAtualizacaoNome(), "Tabela institucional federal parametrizada", autopreenchimento, "criterioAtualizacaoNome"),
                defaultString(request.criterioJurosNome(), "Juros mensais parametrizados", autopreenchimento, "criterioJurosNome"),
                request.observacoesTecnicas()
        );
        return new AutomationEnvelope<>(normalized, autopreenchimento, pendencias, bloqueios, ajustes, confirmacoes);
    }

    private List<String> merge(List<String> left, List<String> right) {
        List<String> merged = new ArrayList<>();
        if (left != null) {
            merged.addAll(left);
        }
        if (right != null) {
            for (String item : right) {
                if (item != null && !item.isBlank() && !merged.contains(item)) {
                    merged.add(item);
                }
            }
        }
        return merged;
    }

    private BigDecimal zeroDecimal(BigDecimal value, Map<String, Object> autopreenchimento, String campo) {
        if (value != null) {
            return value;
        }
        autopreenchimento.put(campo, BigDecimal.ZERO);
        return BigDecimal.ZERO;
    }

    private BigDecimal defaultDecimal(BigDecimal value,
                                      BigDecimal fallback,
                                      Map<String, Object> autopreenchimento,
                                      List<String> ajustes,
                                      String campo,
                                      String ajusteMensagem) {
        if (value != null) {
            return value;
        }
        autopreenchimento.put(campo, fallback);
        if (ajusteMensagem != null && !ajusteMensagem.isBlank()) {
            ajustes.add(ajusteMensagem);
        }
        return fallback;
    }

    private Integer defaultInteger(Integer value,
                                   Integer fallback,
                                   Map<String, Object> autopreenchimento,
                                   List<String> ajustes,
                                   String campo,
                                   String ajusteMensagem) {
        if (value != null) {
            return value;
        }
        autopreenchimento.put(campo, fallback);
        if (ajusteMensagem != null && !ajusteMensagem.isBlank()) {
            ajustes.add(ajusteMensagem);
        }
        return fallback;
    }

    private Boolean defaultBoolean(Boolean value, Boolean fallback, Map<String, Object> autopreenchimento, String campo) {
        if (value != null) {
            return value;
        }
        autopreenchimento.put(campo, fallback);
        return fallback;
    }

    private String defaultString(String value, String fallback, Map<String, Object> autopreenchimento, String campo) {
        if (value != null && !value.isBlank()) {
            return value;
        }
        autopreenchimento.put(campo, fallback);
        return fallback;
    }

    private <T> List<T> emptyList(List<T> value) {
        return value == null ? List.of() : List.copyOf(value);
    }

    private List<CalculoIndiceMensalRequest> emptySeries(List<CalculoIndiceMensalRequest> value) {
        return value == null ? List.of() : List.copyOf(value);
    }


    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record AutomationEnvelope<T>(
            T request,
            Map<String, Object> autopreenchimento,
            List<String> pendencias,
            List<String> bloqueios,
            List<String> ajustes,
            List<String> confirmacoes
    ) {
    }

    private Map<String, Object> safeMetadata(Map<String, Object> metadata) {
        Map<String, Object> safe = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
        safe.entrySet().removeIf(entry -> entry.getValue() == null);
        return Map.copyOf(safe);
    }
}
