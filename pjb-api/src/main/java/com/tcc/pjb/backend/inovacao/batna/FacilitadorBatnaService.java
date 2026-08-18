package com.tcc.pjb.backend.inovacao.batna;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.financeira.router.FinanceiraAiVersionSelector;
import com.tcc.pjb.backend.financial.ai.FinancialAiResponse;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.batna.BatnaRelatorio;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.BatnaRelatorioRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.PropostaAcordoRepository;
import com.tcc.pjb.backend.service.exception.ErroDeTetoException;
import com.tcc.pjb.backend.service.exception.enums.TipoViolacaoTeto;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.service.painel.PainelNacionalJusticaService;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.platform.runtime.execution.PjbTransactionalExecutionSupport;

@Service
public class FacilitadorBatnaService {

    public static final String EVT_BATNA_GERADO = "pjb.inovacao.batna.gerado";
    private static final Logger log = LoggerFactory.getLogger(FacilitadorBatnaService.class);
    private static final BigDecimal BASE_PADRAO = new BigDecimal("10000.00");
    private static final BigDecimal INFLACAO_ANUAL = new BigDecimal("0.045");
    private static final BigDecimal HONORARIOS_BASE = new BigDecimal("0.16");
    private static final BigDecimal FATOR_RECURSO = new BigDecimal("0.22");
    private static final NumberFormat BRL = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
    private static final java.time.Duration BATNA_READ_BUDGET = java.time.Duration.ofSeconds(4);
    private static final java.time.Duration BATNA_WRITE_BUDGET = java.time.Duration.ofSeconds(5);

    private final ProcessoRepository processoRepository;
    private final PropostaAcordoRepository propostaAcordoRepository;
    private final BatnaRelatorioRepository batnaRelatorioRepository;
    private final FinanceiraAiVersionSelector financeiraAiVersionSelector;
    private final PainelNacionalJusticaService painelNacionalJusticaService;
    private final AuditLedgerService auditLedgerService;
    private final OutboxPublisher outboxPublisher;
    private final ObjectMapper objectMapper;
    private final TetoProcessualService tetoProcessualService;
    private final PjbTransactionalExecutionSupport transactionalExecutionSupport;

    public FacilitadorBatnaService(ProcessoRepository processoRepository,
                                   PropostaAcordoRepository propostaAcordoRepository,
                                   BatnaRelatorioRepository batnaRelatorioRepository,
                                   FinanceiraAiVersionSelector financeiraAiVersionSelector,
                                   PainelNacionalJusticaService painelNacionalJusticaService,
                                   AuditLedgerService auditLedgerService,
                                   OutboxPublisher outboxPublisher,
                                   ObjectMapper objectMapper,
                                   TetoProcessualService tetoProcessualService,
                                   PjbTransactionalExecutionSupport transactionalExecutionSupport) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.propostaAcordoRepository = Objects.requireNonNull(propostaAcordoRepository);
        this.batnaRelatorioRepository = Objects.requireNonNull(batnaRelatorioRepository);
        this.financeiraAiVersionSelector = Objects.requireNonNull(financeiraAiVersionSelector);
        this.painelNacionalJusticaService = Objects.requireNonNull(painelNacionalJusticaService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.tetoProcessualService = Objects.requireNonNull(tetoProcessualService);
        this.transactionalExecutionSupport = Objects.requireNonNull(transactionalExecutionSupport);
    }

    public record ContextoProcesso(
            Long processoId,
            Long propostaAcordoId,
            String nupn,
            String tribunalCodigo,
            String ramoDireito,
            String classeTpu,
            BigDecimal valorCausa,
            BigDecimal valorPedidoPrincipal,
            FaseProcessualBatna faseAtual,
            int diasEmAndamento,
            boolean temRecursoProvavel,
            boolean autorAssistidoPorAdvogado,
            boolean reuAssistidoPorAdvogado,
            boolean autorBeneficiarioJg,
            boolean reuBeneficiarioJg,
            boolean autorPessoaJuridica,
            boolean reuPessoaJuridica,
            String uf,
            BigDecimal valorAcordoEmDiscussao,
            boolean modoEstritoTeto
    ) {
    }

    public enum FaseProcessualBatna {
        PRE_DISTRIBUICAO,
        INICIAL,
        INSTRUCAO,
        SANEAMENTO,
        AUDIENCIA_INSTRUCAO,
        SENTENCA_PENDENTE,
        POS_SENTENCA
    }

    public record RelatorioBatna(
            String nupn,
            CustosLitigio custosAutor,
            CustosLitigio custosReu,
            TempoEstimado tempo,
            RiscoRecursal riscoRecursal,
            AnaliseFinanceiraIA analiseFinanceiraIA,
            DiagnosticoTeto diagnosticoTeto,
            DeltaEconomico deltaEconomico,
            List<AlertaBatna> alertas,
            String resumoNarrativo,
            String resumoTecnico,
            double confiancaGeral,
            Instant geradoEm
    ) {
        public BigDecimal custoCombinado() {
            return safe(custosAutor.totalEstimado()).add(safe(custosReu.totalEstimado()));
        }
    }

    public record CustosLitigio(
            String polo,
            BigDecimal honorariosAdvogado,
            BigDecimal custasProcessuais,
            BigDecimal correcaoMonetariaRisco,
            BigDecimal custoOportunidade,
            BigDecimal honorariosSucumbencia,
            BigDecimal cargaRecursal,
            BigDecimal totalEstimado,
            String observacoes
    ) {
    }

    public record TempoEstimado(
            int diasMediaTribunalEstaInstancia,
            int diasMediaComRecursoLocal,
            int diasMediaComRecursoSuperior,
            int diasJaDecorridos,
            int diasRestantesEstimadoSemRecurso,
            int diasRestantesEstimadoComRecurso,
            String fonte
    ) {
    }

    public record RiscoRecursal(
            double probabilidadeRecursoAdverso,
            double probabilidadeReformaSentenca,
            String fundamentoEstatistico,
            boolean apenasInformativo
    ) {
    }

    public record AnaliseFinanceiraIA(
            String origem,
            double confianca,
            BigDecimal custasMin,
            BigDecimal custasMax,
            BigDecimal provisaoMin,
            BigDecimal provisaoMax,
            BigDecimal riscoSucumbenciaEstimado,
            String narrativa
    ) {
    }

    public record DiagnosticoTeto(
            boolean violacao,
            String tipoViolacao,
            String fundamento,
            BigDecimal limiteAplicavel,
            BigDecimal excedente,
            boolean bloqueante
    ) {
        public static DiagnosticoTeto semViolacao() {
            return new DiagnosticoTeto(false, null, "SEM_VIOLACAO", null, BigDecimal.ZERO, false);
        }
    }

    public record DeltaEconomico(
            BigDecimal valorAcordoEmDiscussao,
            BigDecimal custoCombinadoLitigio,
            BigDecimal economiaBrutaPotencial,
            BigDecimal indicePressaoEconomica,
            boolean acordoEconomicamentreSensivel
    ) {
    }

    public record AlertaBatna(
            TipoAlertaBatna tipo,
            String descricao,
            SeveridadeBatna severidade
    ) {
    }

    public enum TipoAlertaBatna {
        TETO_VALOR_CAUSA,
        RAMO_POUCO_CONCILIAVEL,
        RECURSO_PROVAVEL,
        FASE_AVANCADA,
        JUSTICA_GRATUITA,
        VALOR_CAUSA_INCONSISTENTE,
        CUSTO_SUPERA_FAIXA_RAZOAVEL,
        ACORDO_ACIMA_DO_VALOR_CAUSA,
        ACORDO_MUITO_ABAIXO_DA_FRICCAO_LITIGIOSA
    }

    public enum SeveridadeBatna {
        INFO,
        ALERTA,
        CRITICA
    }

    public FaseProcessualBatna parseFase(String valor) {
        if (valor == null || valor.isBlank()) {
            return FaseProcessualBatna.INICIAL;
        }
        String token = valor.trim().toUpperCase(Locale.ROOT);
        return switch (token) {
            case "PRE_DISTRIBUICAO" -> FaseProcessualBatna.PRE_DISTRIBUICAO;
            case "INSTRUCAO", "INSTRUTORIA", "CONHECIMENTO" -> FaseProcessualBatna.INSTRUCAO;
            case "SANEAMENTO" -> FaseProcessualBatna.SANEAMENTO;
            case "AUDIENCIA_INSTRUCAO", "AUDIENCIA_CUSTODIA" -> FaseProcessualBatna.AUDIENCIA_INSTRUCAO;
            case "SENTENCA_PENDENTE" -> FaseProcessualBatna.SENTENCA_PENDENTE;
            case "POS_SENTENCA", "RECURSAL", "CUMPRIMENTO_SENTENCA", "EXECUCAO" -> FaseProcessualBatna.POS_SENTENCA;
            default -> FaseProcessualBatna.INICIAL;
        };
    }

    public RelatorioBatna gerarParaProcesso(Long processoId, BigDecimal valorAcordoEmDiscussao, boolean modoEstritoTeto) {
        ContextoProcesso contexto = transactionalExecutionSupport.executeReadOnly(
                "batna.report.load-process-context",
                BATNA_READ_BUDGET,
                () -> {
                    Processo processo = processoRepository.findProcessoCompletoById(processoId)
                            .orElseThrow(() -> new IllegalArgumentException("Processo nao encontrado: " + processoId));
                    return derivarContexto(processo, null, valorAcordoEmDiscussao, modoEstritoTeto);
                }
        );
        return gerar(contexto);
    }

    public RelatorioBatna gerarParaProcessoEProposta(Long processoId,
                                                     Long propostaAcordoId,
                                                     BigDecimal valorAcordoEmDiscussao,
                                                     boolean modoEstritoTeto) {
        ContextoProcesso contexto = transactionalExecutionSupport.executeReadOnly(
                "batna.report.load-process-proposal-context",
                BATNA_READ_BUDGET,
                () -> {
                    Processo processo = processoRepository.findProcessoCompletoById(processoId)
                            .orElseThrow(() -> new IllegalArgumentException("Processo nao encontrado: " + processoId));
                    return derivarContexto(processo, propostaAcordoId, valorAcordoEmDiscussao, modoEstritoTeto);
                }
        );
        return gerar(contexto);
    }

    @Transactional(readOnly = true)
    public Optional<RelatorioBatna> buscarUltimoPorProcesso(Long processoId) {
        return batnaRelatorioRepository.findTopByProcessoIdOrderByGeradoEmDesc(processoId)
                .flatMap(this::deserialize);
    }

    @Transactional(readOnly = true)
    public Optional<RelatorioBatna> buscarUltimoPorProposta(Long propostaId) {
        return batnaRelatorioRepository.findTopByPropostaAcordoIdOrderByGeradoEmDesc(propostaId)
                .flatMap(this::deserialize);
    }

    public RelatorioBatna gerar(ContextoProcesso contextoBruto) {
        ContextoProcesso ctx = transactionalExecutionSupport.executeReadOnly(
                "batna.report.normalize-context",
                BATNA_READ_BUDGET,
                () -> normalizar(contextoBruto)
        );
        DiagnosticoTeto teto = diagnosticarTeto(ctx);
        if (ctx.modoEstritoTeto() && teto.violacao()) {
            throw buildErroDeTeto(teto, ctx.valorCausa());
        }

        AnaliseFinanceiraIA analiseFinanceiraIA = executarAnaliseFinanceira(ctx);
        TempoEstimado tempo = estimarTempo(ctx);
        RiscoRecursal riscoRecursal = estimarRiscoRecursal(ctx);
        CustosLitigio custosAutor = calcularCustos("AUTOR", ctx, tempo, riscoRecursal, analiseFinanceiraIA, true);
        CustosLitigio custosReu = calcularCustos("REU", ctx, tempo, riscoRecursal, analiseFinanceiraIA, false);
        DeltaEconomico deltaEconomico = calcularDeltaEconomico(ctx, custosAutor, custosReu);
        List<AlertaBatna> alertas = gerarAlertas(ctx, teto, riscoRecursal, custosAutor, custosReu, deltaEconomico);
        double confianca = calcularConfianca(ctx, analiseFinanceiraIA, teto, alertas);
        String resumoNarrativo = construirResumoNarrativo(ctx, custosAutor, custosReu, tempo, deltaEconomico, alertas);
        String resumoTecnico = construirResumoTecnico(ctx, custosAutor, custosReu, tempo, riscoRecursal, analiseFinanceiraIA, teto, deltaEconomico, confianca);

        RelatorioBatna relatorio = new RelatorioBatna(
                ctx.nupn(),
                custosAutor,
                custosReu,
                tempo,
                riscoRecursal,
                analiseFinanceiraIA,
                teto,
                deltaEconomico,
                Collections.unmodifiableList(alertas),
                resumoNarrativo,
                resumoTecnico,
                confianca,
                Instant.now()
        );

        transactionalExecutionSupport.executeInNewTransaction(
                "batna.report.persist",
                BATNA_WRITE_BUDGET,
                () -> persistir(ctx, relatorio)
        );
        publicar(ctx, relatorio);
        return relatorio;
    }

    private ContextoProcesso derivarContexto(Processo processo,
                                             Long propostaAcordoId,
                                             BigDecimal valorAcordoEmDiscussao,
                                             boolean modoEstritoTeto) {
        String tribunalCodigo = resolveTribunalCodigo(processo);
        String ramo = processo.getRamoDireito() != null ? processo.getRamoDireito().name() : "CIVIL";
        String uf = resolveUf(processo);
        BigDecimal valorCausa = processo.getValorCausa() != null ? processo.getValorCausa() : BASE_PADRAO;
        return new ContextoProcesso(
                processo.getId(),
                propostaAcordoId,
                processo.getNumeroUnificado(),
                tribunalCodigo,
                ramo,
                processo.getClasseProcessual(),
                valorCausa,
                valorCausa,
                mapFase(processo),
                diasEmAndamento(processo),
                recursoProvavel(processo.getRamoDireito(), processo.getFaseAtual()),
                true,
                true,
                false,
                false,
                false,
                false,
                uf,
                valorAcordoEmDiscussao,
                modoEstritoTeto
        );
    }

    private ContextoProcesso normalizar(ContextoProcesso ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("Contexto BATNA obrigatorio");
        }
        Long processoId = ctx.processoId();
        Long propostaAcordoId = ctx.propostaAcordoId();
        Processo processo = null;
        if (processoId != null) {
            processo = processoRepository.findProcessoCompletoById(processoId).orElse(null);
        } else if (propostaAcordoId != null) {
            PropostaAcordo proposta = propostaAcordoRepository.findById(propostaAcordoId).orElse(null);
            if (proposta != null) {
                processo = proposta.getProcesso();
                processoId = processo != null ? processo.getId() : null;
            }
        }

        String nupn = firstNonBlank(ctx.nupn(), processo != null ? processo.getNumeroUnificado() : null, "NUPN-PENDENTE");
        String tribunalCodigo = firstNonBlank(ctx.tribunalCodigo(), resolveTribunalCodigo(processo), "N/D");
        String ramo = firstNonBlank(ctx.ramoDireito(), processo != null && processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null, "CIVIL");
        BigDecimal valorCausa = firstPositive(ctx.valorCausa(), processo != null ? processo.getValorCausa() : null, BASE_PADRAO);
        BigDecimal valorPedidoPrincipal = firstPositive(ctx.valorPedidoPrincipal(), valorCausa, BASE_PADRAO);
        FaseProcessualBatna fase = ctx.faseAtual() != null ? ctx.faseAtual() : (processo != null ? mapFase(processo) : FaseProcessualBatna.INICIAL);
        int dias = ctx.diasEmAndamento() > 0 ? ctx.diasEmAndamento() : (processo != null ? diasEmAndamento(processo) : 0);
        boolean recurso = ctx.temRecursoProvavel() || (processo != null && recursoProvavel(processo.getRamoDireito(), processo.getFaseAtual()));
        String uf = firstNonBlank(ctx.uf(), resolveUf(processo), "BR");
        return new ContextoProcesso(
                processoId,
                propostaAcordoId,
                nupn,
                tribunalCodigo,
                ramo,
                firstNonBlank(ctx.classeTpu(), processo != null ? processo.getClasseProcessual() : null, "PROCEDIMENTO_COMUM"),
                decimal(valorCausa),
                decimal(valorPedidoPrincipal),
                fase,
                Math.max(0, dias),
                recurso,
                ctx.autorAssistidoPorAdvogado(),
                ctx.reuAssistidoPorAdvogado(),
                ctx.autorBeneficiarioJg(),
                ctx.reuBeneficiarioJg(),
                ctx.autorPessoaJuridica(),
                ctx.reuPessoaJuridica(),
                uf,
                decimalOrNull(ctx.valorAcordoEmDiscussao()),
                ctx.modoEstritoTeto()
        );
    }

    private AnaliseFinanceiraIA executarAnaliseFinanceira(ContextoProcesso ctx) {
        IARequest request = IARequest.builder()
                .origem("BATNA")
                .acao("ESTIMATIVA_LITIGIO")
                .payload("ramoDireito", ctx.ramoDireito())
                .payload("ramo_direito", ctx.ramoDireito())
                .payload("valorCausa", ctx.valorCausa())
                .payload("classeTPU", ctx.classeTpu())
                .payload("faseProcessual", ctx.faseAtual().name())
                .payload("tribunalCodigo", ctx.tribunalCodigo())
                .payload("uf", ctx.uf())
                .build();
        FinancialAiResponse response = financeiraAiVersionSelector.processUnified(request, ApiVersion.latest(), request.getAcao());
        Map<String, Object> custasRange = response != null ? response.outputMap("custas_range") : Map.of();
        Map<String, Object> provisaoRange = response != null ? response.outputMap("provisao_range") : Map.of();
        BigDecimal riscoSucumbencia = percentageToValue(number(response != null ? response.output("risco_sucumbencia") : null), ctx.valorCausa());
        return new AnaliseFinanceiraIA(
                response != null ? response.origin() : "FINANCEIRA_V3",
                response != null ? response.confidence() : 0.62d,
                decimal(number(custasRange.get("min"))),
                decimal(number(custasRange.get("max"))),
                decimal(number(provisaoRange.get("min"))),
                decimal(number(provisaoRange.get("max"))),
                decimal(riscoSucumbencia),
                response != null ? response.messageOr("Estimativa financeira indisponivel") : "Estimativa financeira indisponivel"
        );
    }

    private TempoEstimado estimarTempo(ContextoProcesso ctx) {
        double mediaPainel = painelNacionalJusticaService.metricasTribunal(ctx.tribunalCodigo())
                .map(PainelNacionalJusticaService.MetricaTribunal::tempoMedioResolucaoDias)
                .orElse(defaultDiasPorRamo(ctx.ramoDireito()));
        int base = (int) Math.max(180d, mediaPainel > 0d ? mediaPainel : defaultDiasPorRamo(ctx.ramoDireito()));
        int faseRestante = switch (ctx.faseAtual()) {
            case PRE_DISTRIBUICAO -> base;
            case INICIAL -> (int) (base * 0.85d);
            case INSTRUCAO -> (int) (base * 0.55d);
            case SANEAMENTO -> (int) (base * 0.45d);
            case AUDIENCIA_INSTRUCAO -> (int) (base * 0.30d);
            case SENTENCA_PENDENTE -> (int) (base * 0.12d);
            case POS_SENTENCA -> 120;
        };
        int semRecurso = Math.max(45, faseRestante);
        int comRecurso = ctx.temRecursoProvavel() ? semRecurso + recursalDiasPorRamo(ctx.ramoDireito()) : semRecurso + 90;
        int superior = comRecurso + 540;
        return new TempoEstimado(
                base,
                comRecurso,
                superior,
                ctx.diasEmAndamento(),
                semRecurso,
                comRecurso,
                "PainelNacionalJusticaService + heuristica por ramo e fase"
        );
    }

    private RiscoRecursal estimarRiscoRecursal(ContextoProcesso ctx) {
        double probRecurso = switch (normalizeToken(ctx.ramoDireito())) {
            case "FAZENDA", "ADMINISTRATIVO", "TRIBUTARIO", "PREVIDENCIARIO" -> 0.78d;
            case "PENAL" -> 0.65d;
            case "TRABALHISTA" -> 0.58d;
            case "CONSUMIDOR" -> 0.46d;
            case "FAMILIA" -> 0.40d;
            default -> 0.52d;
        };
        if (ctx.temRecursoProvavel()) {
            probRecurso = Math.min(0.95d, probRecurso + 0.08d);
        }
        double probReforma = switch (normalizeToken(ctx.ramoDireito())) {
            case "TRABALHISTA" -> 0.31d;
            case "ADMINISTRATIVO", "TRIBUTARIO", "PREVIDENCIARIO" -> 0.28d;
            case "CONSUMIDOR" -> 0.19d;
            default -> 0.24d;
        };
        return new RiscoRecursal(
                probRecurso,
                probReforma,
                "Serie historica agregada por ramo, fase e intensidade recursal do projeto",
                true
        );
    }

    private CustosLitigio calcularCustos(String polo,
                                         ContextoProcesso ctx,
                                         TempoEstimado tempo,
                                         RiscoRecursal riscoRecursal,
                                         AnaliseFinanceiraIA analiseFinanceiraIA,
                                         boolean autor) {
        BigDecimal valorBase = firstPositive(ctx.valorPedidoPrincipal(), ctx.valorCausa(), BASE_PADRAO);
        boolean assistido = autor ? ctx.autorAssistidoPorAdvogado() : ctx.reuAssistidoPorAdvogado();
        boolean beneficiarioJg = autor ? ctx.autorBeneficiarioJg() : ctx.reuBeneficiarioJg();
        boolean pessoaJuridica = autor ? ctx.autorPessoaJuridica() : ctx.reuPessoaJuridica();

        BigDecimal honorarios = assistido
                ? decimal(valorBase.multiply(HONORARIOS_BASE).multiply(stageFactor(ctx.faseAtual())))
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal custas = beneficiarioJg
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : decimal(avg(analiseFinanceiraIA.custasMin(), analiseFinanceiraIA.custasMax()).multiply(stageFactor(ctx.faseAtual())));
        BigDecimal correcao = decimal(valorBase
                .multiply(INFLACAO_ANUAL)
                .multiply(BigDecimal.valueOf(Math.max(tempo.diasRestantesEstimadoSemRecurso(), 30) / 365.0d)));
        BigDecimal valorHora = pessoaJuridica ? new BigDecimal("240.00") : new BigDecimal("65.00");
        BigDecimal horas = BigDecimal.valueOf(Math.max(12d, tempo.diasRestantesEstimadoSemRecurso() * (pessoaJuridica ? 0.22d : 0.10d)));
        BigDecimal custoOportunidade = decimal(horas.multiply(valorHora));
        BigDecimal sucumbencia = decimal(analiseFinanceiraIA.riscoSucumbenciaEstimado().multiply(BigDecimal.valueOf(riscoRecursal.probabilidadeReformaSentenca() + 0.45d)));
        BigDecimal cargaRecursal = ctx.temRecursoProvavel()
                ? decimal(avg(analiseFinanceiraIA.provisaoMin(), analiseFinanceiraIA.provisaoMax())
                    .multiply(FATOR_RECURSO)
                    .multiply(BigDecimal.valueOf(riscoRecursal.probabilidadeRecursoAdverso())))
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = decimal(honorarios.add(custas).add(correcao).add(custoOportunidade).add(sucumbencia).add(cargaRecursal));
        String observacoes = beneficiarioJg ? "Custas estimadas neutralizadas por justica gratuita" : "Custos agregados por estatistica, fase e friccao recursal";
        return new CustosLitigio(polo, honorarios, custas, correcao, custoOportunidade, sucumbencia, cargaRecursal, total, observacoes);
    }

    private DeltaEconomico calcularDeltaEconomico(ContextoProcesso ctx,
                                                  CustosLitigio custosAutor,
                                                  CustosLitigio custosReu) {
        BigDecimal acordo = decimalOrNull(ctx.valorAcordoEmDiscussao());
        BigDecimal custoCombinado = safe(custosAutor.totalEstimado()).add(safe(custosReu.totalEstimado()));
        BigDecimal economia = acordo != null ? decimal(custoCombinado.subtract(acordo)) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal indicePressao = ctx.valorCausa() != null && ctx.valorCausa().compareTo(BigDecimal.ZERO) > 0
                ? decimal(custoCombinado.divide(ctx.valorCausa(), 6, RoundingMode.HALF_UP))
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        boolean sensivel = acordo != null && acordo.compareTo(custoCombinado) <= 0;
        return new DeltaEconomico(acordo, custoCombinado, economia, indicePressao, sensivel);
    }

    private List<AlertaBatna> gerarAlertas(ContextoProcesso ctx,
                                           DiagnosticoTeto teto,
                                           RiscoRecursal riscoRecursal,
                                           CustosLitigio custosAutor,
                                           CustosLitigio custosReu,
                                           DeltaEconomico deltaEconomico) {
        List<AlertaBatna> alertas = new ArrayList<>();
        if (teto.violacao()) {
            alertas.add(new AlertaBatna(TipoAlertaBatna.TETO_VALOR_CAUSA, teto.fundamento(), teto.bloqueante() ? SeveridadeBatna.CRITICA : SeveridadeBatna.ALERTA));
        }
        RamoDireito ramo = RamoDireito.fromString(ctx.ramoDireito());
        if (ramo != null && !ramo.admiteConciliacao()) {
            alertas.add(new AlertaBatna(TipoAlertaBatna.RAMO_POUCO_CONCILIAVEL, "Ramo com baixa vocacao conciliatoria institucional", SeveridadeBatna.ALERTA));
        }
        if (ctx.temRecursoProvavel() || riscoRecursal.probabilidadeRecursoAdverso() >= 0.70d) {
            alertas.add(new AlertaBatna(TipoAlertaBatna.RECURSO_PROVAVEL, "Friccao recursal relevante, com aumento de tempo e caixa comprometido", SeveridadeBatna.ALERTA));
        }
        if (ctx.faseAtual() == FaseProcessualBatna.POS_SENTENCA || ctx.faseAtual() == FaseProcessualBatna.SENTENCA_PENDENTE) {
            alertas.add(new AlertaBatna(TipoAlertaBatna.FASE_AVANCADA, "Processo em faixa avancada, com custo marginal crescente", SeveridadeBatna.INFO));
        }
        if (ctx.autorBeneficiarioJg() || ctx.reuBeneficiarioJg()) {
            alertas.add(new AlertaBatna(TipoAlertaBatna.JUSTICA_GRATUITA, "Justica gratuita altera a matriz de custas e pode distorcer a comparacao simetrica", SeveridadeBatna.INFO));
        }
        BigDecimal custoCombinado = safe(custosAutor.totalEstimado()).add(safe(custosReu.totalEstimado()));
        if (ctx.valorCausa() == null || ctx.valorCausa().compareTo(BigDecimal.ZERO) <= 0) {
            alertas.add(new AlertaBatna(TipoAlertaBatna.VALOR_CAUSA_INCONSISTENTE, "Valor da causa ausente ou inconsistente, usando base conservadora", SeveridadeBatna.ALERTA));
        } else if (custoCombinado.compareTo(ctx.valorCausa().multiply(new BigDecimal("0.40"))) > 0) {
            alertas.add(new AlertaBatna(TipoAlertaBatna.CUSTO_SUPERA_FAIXA_RAZOAVEL, "Atrito economico do litigio supera 40% do valor da causa", SeveridadeBatna.ALERTA));
        }
        if (ctx.valorAcordoEmDiscussao() != null && ctx.valorCausa() != null && ctx.valorAcordoEmDiscussao().compareTo(ctx.valorCausa().multiply(new BigDecimal("1.20"))) > 0) {
            alertas.add(new AlertaBatna(TipoAlertaBatna.ACORDO_ACIMA_DO_VALOR_CAUSA, "Valor em discussao supera materialmente a base economica da causa", SeveridadeBatna.CRITICA));
        }
        if (ctx.valorAcordoEmDiscussao() != null && ctx.valorAcordoEmDiscussao().compareTo(custoCombinado.multiply(new BigDecimal("0.25"))) < 0) {
            alertas.add(new AlertaBatna(TipoAlertaBatna.ACORDO_MUITO_ABAIXO_DA_FRICCAO_LITIGIOSA, "Valor em discussao esta muito abaixo da friccao economica total do litigio", SeveridadeBatna.ALERTA));
        }
        return alertas;
    }

    private double calcularConfianca(ContextoProcesso ctx,
                                     AnaliseFinanceiraIA analiseFinanceiraIA,
                                     DiagnosticoTeto teto,
                                     List<AlertaBatna> alertas) {
        double confianca = analiseFinanceiraIA.confianca();
        if (ctx.processoId() != null) {
            confianca += 0.06d;
        }
        if (ctx.valorCausa() != null && ctx.valorCausa().compareTo(BigDecimal.ZERO) > 0) {
            confianca += 0.05d;
        }
        if (ctx.valorAcordoEmDiscussao() != null) {
            confianca += 0.04d;
        }
        if (teto.violacao()) {
            confianca -= 0.08d;
        }
        confianca -= alertas.stream().mapToDouble(a -> a.severidade() == SeveridadeBatna.CRITICA ? 0.05d : a.severidade() == SeveridadeBatna.ALERTA ? 0.02d : 0.01d).sum();
        return Math.max(0.40d, Math.min(0.97d, confianca));
    }

    private String construirResumoNarrativo(ContextoProcesso ctx,
                                            CustosLitigio autor,
                                            CustosLitigio reu,
                                            TempoEstimado tempo,
                                            DeltaEconomico delta,
                                            List<AlertaBatna> alertas) {
        StringBuilder sb = new StringBuilder();
        sb.append("O processo esta em curso ha ").append(ctx.diasEmAndamento()).append(" dias. ");
        sb.append("A projecao estatistica indica mais ").append(tempo.diasRestantesEstimadoSemRecurso()).append(" dias ate uma saida ordinaria, ");
        sb.append("ou cerca de ").append(tempo.diasRestantesEstimadoComRecurso()).append(" dias com friccao recursal. ");
        sb.append("O custo economico estimado do litigio e de ").append(formatCurrency(autor.totalEstimado())).append(" para o autor e ");
        sb.append(formatCurrency(reu.totalEstimado())).append(" para o reu. ");
        if (delta.valorAcordoEmDiscussao() != null) {
            sb.append("Com o valor em discussao de ").append(formatCurrency(delta.valorAcordoEmDiscussao())).append(", a diferenca frente ao custo combinado do litigio e de ");
            sb.append(formatCurrency(delta.economiaBrutaPotencial())).append(". ");
        }
        if (!alertas.isEmpty()) {
            sb.append("Foram identificados ").append(alertas.size()).append(" alertas economico-processuais relevantes. ");
        }
        sb.append("O relatorio e informativo e nao antecipa resultado judicial.");
        return sb.toString();
    }

    private String construirResumoTecnico(ContextoProcesso ctx,
                                          CustosLitigio autor,
                                          CustosLitigio reu,
                                          TempoEstimado tempo,
                                          RiscoRecursal riscoRecursal,
                                          AnaliseFinanceiraIA analiseFinanceiraIA,
                                          DiagnosticoTeto teto,
                                          DeltaEconomico delta,
                                          double confianca) {
        return "BATNA|NUPN=" + ctx.nupn()
                + "|TRIBUNAL=" + ctx.tribunalCodigo()
                + "|RAMO=" + ctx.ramoDireito()
                + "|FASE=" + ctx.faseAtual().name()
                + "|TEMPO_SEM_RECURSO=" + tempo.diasRestantesEstimadoSemRecurso()
                + "|TEMPO_COM_RECURSO=" + tempo.diasRestantesEstimadoComRecurso()
                + "|CUSTO_AUTOR=" + autor.totalEstimado().toPlainString()
                + "|CUSTO_REU=" + reu.totalEstimado().toPlainString()
                + "|CUSTO_COMBINADO=" + delta.custoCombinadoLitigio().toPlainString()
                + "|VALOR_ACORDO=" + (delta.valorAcordoEmDiscussao() != null ? delta.valorAcordoEmDiscussao().toPlainString() : "NA")
                + "|PRESSAO_ECONOMICA=" + delta.indicePressaoEconomica().toPlainString()
                + "|P_RECURSO=" + percent(riscoRecursal.probabilidadeRecursoAdverso())
                + "|P_REFORMA=" + percent(riscoRecursal.probabilidadeReformaSentenca())
                + "|FINANCEIRA=" + analiseFinanceiraIA.origem()
                + "|TETO=" + teto.violacao()
                + "|CONFIANCA=" + percent(confianca);
    }

    private DiagnosticoTeto diagnosticarTeto(ContextoProcesso ctx) {
        Processo processo = ctx.processoId() != null ? processoRepository.findProcessoCompletoById(ctx.processoId()).orElse(null) : null;
        TetoProcessualService.DiagnosticoTetoProcessual diagnostico = tetoProcessualService.diagnosticar(
                ctx.valorCausa(),
                processo != null ? processo.getTipoJustica() : null,
                processo != null ? processo.getRamoDireito() : null,
                processo != null ? processo.getRito() : null,
                processo != null ? processo.getJurisdicao() : null,
                processo != null && processo.getDataCriacao() != null ? processo.getDataCriacao().toLocalDate() : java.time.LocalDate.now()
        );
        if (!diagnostico.violacao()) {
            return DiagnosticoTeto.semViolacao();
        }
        return new DiagnosticoTeto(
                true,
                diagnostico.tipoViolacao() != null ? diagnostico.tipoViolacao().name() : null,
                diagnostico.fundamentoLegal(),
                decimal(diagnostico.limiteLegal()),
                decimal(diagnostico.excedente()),
                diagnostico.bloqueante()
        );
    }

    private void persistir(ContextoProcesso ctx, RelatorioBatna relatorio) {
        BatnaRelatorio entity = new BatnaRelatorio();
        if (ctx.processoId() != null) {
            processoRepository.findById(ctx.processoId()).ifPresent(entity::setProcesso);
        }
        if (ctx.propostaAcordoId() != null) {
            propostaAcordoRepository.findById(ctx.propostaAcordoId()).ifPresent(entity::setPropostaAcordo);
        }
        entity.setNupn(ctx.nupn());
        entity.setTribunalCodigo(ctx.tribunalCodigo());
        entity.setRamoDireito(ctx.ramoDireito());
        entity.setClasseTpu(ctx.classeTpu());
        entity.setFaseProcessual(ctx.faseAtual().name());
        entity.setValorCausa(decimal(ctx.valorCausa()));
        entity.setValorPedidoPrincipal(decimal(ctx.valorPedidoPrincipal()));
        entity.setValorAcordoEmDiscussao(decimalOrNull(ctx.valorAcordoEmDiscussao()));
        entity.setCustoAutorTotal(decimal(relatorio.custosAutor().totalEstimado()));
        entity.setCustoReuTotal(decimal(relatorio.custosReu().totalEstimado()));
        entity.setCustoCombinado(decimal(relatorio.custoCombinado()));
        entity.setDiasRestantesSemRecurso(relatorio.tempo().diasRestantesEstimadoSemRecurso());
        entity.setDiasRestantesComRecurso(relatorio.tempo().diasRestantesEstimadoComRecurso());
        entity.setProbabilidadeRecurso(decimal(BigDecimal.valueOf(relatorio.riscoRecursal().probabilidadeRecursoAdverso())));
        entity.setProbabilidadeReforma(decimal(BigDecimal.valueOf(relatorio.riscoRecursal().probabilidadeReformaSentenca())));
        entity.setIndicePressaoEconomica(decimal(relatorio.deltaEconomico().indicePressaoEconomica()));
        entity.setConfiancaGeral(decimal(BigDecimal.valueOf(relatorio.confiancaGeral())));
        entity.setTetoViolacao(relatorio.diagnosticoTeto().violacao());
        entity.setTetoTipo(relatorio.diagnosticoTeto().tipoViolacao());
        entity.setTetoLimite(decimalOrNull(relatorio.diagnosticoTeto().limiteAplicavel()));
        entity.setTetoExcedente(decimalOrNull(relatorio.diagnosticoTeto().excedente()));
        entity.setResumoNarrativo(relatorio.resumoNarrativo());
        entity.setResumoTecnico(relatorio.resumoTecnico());
        entity.setHashContexto(Hashes.sha256Hex(writeValue(ctx)));
        entity.setRequestJson(writeValue(ctx));
        entity.setResponseJson(writeValue(relatorio));
        entity.setGeradoEm(relatorio.geradoEm());
        batnaRelatorioRepository.save(entity);
    }

    private void publicar(ContextoProcesso ctx, RelatorioBatna relatorio) {
        String payload = writeValue(relatorio);
        auditLedgerService.appendSafely("BATNA_REPORT_GENERATED", "BATNA", ctx.nupn(), payload);
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("nupn", ctx.nupn());
        event.put("processoId", ctx.processoId());
        event.put("propostaAcordoId", ctx.propostaAcordoId());
        event.put("tribunalCodigo", ctx.tribunalCodigo());
        event.put("ramoDireito", ctx.ramoDireito());
        event.put("valorCausa", ctx.valorCausa());
        event.put("valorAcordoEmDiscussao", ctx.valorAcordoEmDiscussao());
        event.put("custoCombinado", relatorio.custoCombinado());
        event.put("confianca", relatorio.confiancaGeral());
        event.put("tetoViolacao", relatorio.diagnosticoTeto().violacao());
        event.put("geradoEm", relatorio.geradoEm());
        outboxPublisher.enqueue(
                ctx.nupn(),
                EVT_BATNA_GERADO,
                event,
                Map.of("modulo", "BATNA", "tribunalCodigo", ctx.tribunalCodigo()),
                "batna:" + Hashes.sha256Hex(payload),
                "PROCESSO",
                ctx.processoId() != null ? Long.toString(ctx.processoId()) : ctx.nupn()
        );
    }

    private Optional<RelatorioBatna> deserialize(BatnaRelatorio entity) {
        try {
            return Optional.of(objectMapper.readValue(entity.getResponseJson(), RelatorioBatna.class));
        } catch (Exception e) {
            log.warn("Falha ao desserializar BATNA {}: {}", entity.getId(), e.getMessage());
            return Optional.empty();
        }
    }

    private ErroDeTetoException buildErroDeTeto(DiagnosticoTeto teto, BigDecimal valorCausa) {
        TipoViolacaoTeto tipo = Optional.ofNullable(teto.tipoViolacao())
                .map(v -> {
                    try {
                        return TipoViolacaoTeto.valueOf(v);
                    } catch (Exception ignored) {
                        return TipoViolacaoTeto.RITO_INCOMPATIVEL;
                    }
                })
                .orElse(TipoViolacaoTeto.RITO_INCOMPATIVEL);
        return new ErroDeTetoException.Builder(tipo)
                .fundamento(teto.fundamento())
                .calculoFinanceiro(safe(teto.limiteAplicavel()), safe(valorCausa))
                .sugestao("Adequar valor da causa, rito ou jurisdicao antes de impulsionar proposta negocial")
                .build();
    }


    private static FaseProcessualBatna mapFase(Processo processo) {
        if (processo == null || processo.getFaseAtual() == null) {
            return FaseProcessualBatna.INICIAL;
        }
        String nome = processo.getFaseAtual().name();
        return switch (nome) {
            case "CONHECIMENTO", "COGNITIVA" -> FaseProcessualBatna.INICIAL;
            case "INSTRUTORIA", "PERICIA_TECNICA" -> FaseProcessualBatna.INSTRUCAO;
            case "AUDIENCIA_CUSTODIA" -> FaseProcessualBatna.AUDIENCIA_INSTRUCAO;
            case "RECURSAL", "EXECUCAO", "CUMPRIMENTO_SENTENCA", "EXECUTORIA", "PENHORA" -> FaseProcessualBatna.POS_SENTENCA;
            default -> FaseProcessualBatna.SANEAMENTO;
        };
    }

    private static boolean recursoProvavel(RamoDireito ramo, com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual faseAtual) {
        if (faseAtual != null && (faseAtual.name().contains("RECURSAL") || faseAtual.name().contains("SENTEN"))) {
            return true;
        }
        if (ramo == null) {
            return false;
        }
        return switch (ramo) {
            case ADMINISTRATIVO, TRIBUTARIO, PREVIDENCIARIO, PENAL -> true;
            default -> false;
        };
    }

    private static int diasEmAndamento(Processo processo) {
        LocalDateTime inicio = processo != null ? processo.getDataCriacao() : null;
        if (inicio == null && processo != null) {
            inicio = processo.getDataDistribuicao();
        }
        if (inicio == null) {
            return 0;
        }
        return (int) Math.max(0L, ChronoUnit.DAYS.between(inicio, LocalDateTime.now()));
    }

    private static String resolveTribunalCodigo(Processo processo) {
        if (processo == null) {
            return "N/D";
        }
        String routed = firstNonBlank(processo.getTribunalCodigoRoteado(), null);
        if (routed != null) {
            return routed;
        }
        var jurisdicao = processo.getJurisdicao();
        if (jurisdicao == null) {
            return "N/D";
        }
        return firstNonBlank(jurisdicao.getSigla(), jurisdicao.getCodigo(), "N/D");
    }

    private static String resolveUf(Processo processo) {
        if (processo == null || processo.getJurisdicao() == null) {
            return null;
        }
        return firstNonBlank(processo.getJurisdicao().getUf(), null);
    }

    private static double defaultDiasPorRamo(String ramoDireito) {
        return switch (normalizeToken(ramoDireito)) {
            case "TRABALHISTA" -> 720d;
            case "PENAL" -> 900d;
            case "FAZENDA", "ADMINISTRATIVO", "PREVIDENCIARIO", "TRIBUTARIO" -> 1440d;
            case "FAMILIA" -> 540d;
            case "CONSUMIDOR" -> 360d;
            default -> 730d;
        };
    }

    private static int recursalDiasPorRamo(String ramoDireito) {
        return switch (normalizeToken(ramoDireito)) {
            case "ADMINISTRATIVO", "TRIBUTARIO", "PREVIDENCIARIO", "FAZENDA" -> 720;
            case "PENAL" -> 600;
            case "TRABALHISTA" -> 420;
            default -> 540;
        };
    }

    private static BigDecimal stageFactor(FaseProcessualBatna fase) {
        return switch (fase) {
            case PRE_DISTRIBUICAO -> new BigDecimal("0.80");
            case INICIAL -> new BigDecimal("0.95");
            case INSTRUCAO -> new BigDecimal("1.10");
            case SANEAMENTO -> new BigDecimal("1.00");
            case AUDIENCIA_INSTRUCAO -> new BigDecimal("1.18");
            case SENTENCA_PENDENTE -> new BigDecimal("1.05");
            case POS_SENTENCA -> new BigDecimal("1.30");
        };
    }

    private static BigDecimal avg(BigDecimal a, BigDecimal b) {
        return decimal(safe(a).add(safe(b)).divide(new BigDecimal("2"), 6, RoundingMode.HALF_UP));
    }

    private static BigDecimal percentageToValue(Double percentual, BigDecimal valorBase) {
        if (percentual == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return decimal(safe(valorBase).multiply(BigDecimal.valueOf(percentual)));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private static Double number(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Double.parseDouble(s.replace(',', '.'));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String writeValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("json batna", e);
        }
    }

    private static BigDecimal firstPositive(BigDecimal... values) {
        if (values == null) {
            return BASE_PADRAO;
        }
        for (BigDecimal value : values) {
            if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                return decimal(value);
            }
        }
        return BASE_PADRAO;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String normalizeToken(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT)
                .replace('Á', 'A')
                .replace('Â', 'A')
                .replace('Ã', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ô', 'O')
                .replace('Õ', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C');
    }

    private static BigDecimal decimal(BigDecimal value) {
        return safe(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal decimal(Double value) {
        if (value == null || !Double.isFinite(value)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal decimalOrNull(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String formatCurrency(BigDecimal value) {
        synchronized (BRL) {
            return BRL.format(safe(value));
        }
    }

    private static String percent(double value) {
        return String.format(Locale.ROOT, "%.0f%%", value * 100d);
    }
}
