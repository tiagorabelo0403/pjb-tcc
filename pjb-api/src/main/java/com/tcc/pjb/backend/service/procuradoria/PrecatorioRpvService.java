package com.tcc.pjb.backend.service.procuradoria;

import com.tcc.pjb.backend.model.dto.procuradoria.surface.PrecatorioRpvEnteDevedorTipo;
import com.tcc.pjb.backend.model.dto.procuradoria.surface.PrecatorioRpvNaturezaCredito;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.procuradoria.calendar.PrecatorioRpvCalendarPlanner;
import com.tcc.pjb.backend.service.procuradoria.queue.PrecatorioRpvQueuePlanner;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrecatorioRpvService {

    private static final LocalDate MARCO_FEDERAL_EC_136 = LocalDate.of(2025, 9, 1);
    private static final LocalDate MARCO_SUBNACIONAL_EC_136 = LocalDate.of(2025, 8, 1);
    private static final BigDecimal JUROS_DOIS_POR_CENTO_AO_ANO = new BigDecimal("0.02");

    private final ProcessoRepository processoRepository;
    private final PrecatorioRpvQueuePlanner queuePlanner;
    private final PrecatorioRpvCalendarPlanner calendarPlanner;

    public PrecatorioRpvService(ProcessoRepository processoRepository,
                                PrecatorioRpvQueuePlanner queuePlanner,
                                PrecatorioRpvCalendarPlanner calendarPlanner) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.queuePlanner = Objects.requireNonNull(queuePlanner);
        this.calendarPlanner = Objects.requireNonNull(calendarPlanner);
    }

    @Transactional(readOnly = true)
    public PrecatorioRpvResponse calcular(PrecatorioRpvRequest request) {
        Processo processo = processoRepository.findById(request.processoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", request.processoId()));
        LocalDate calculadoEm = request.dataBaseCalculo() == null ? LocalDate.now() : request.dataBaseCalculo();
        PrecatorioRpvNaturezaCredito naturezaCredito = resolveNaturezaCredito(request);
        PrecatorioRpvEnteDevedorTipo enteDevedorTipo = resolveEnteDevedorTipo(request);
        BigDecimal principal = resolvePrincipal(request, processo);
        MonetaryComputation monetaryComputation = computeMonetary(principal, request, naturezaCredito, enteDevedorTipo, calculadoEm);
        BigDecimal limiteRpv = safe(request.limiteRpv());
        String modalidade = limiteRpv.signum() > 0 && monetaryComputation.totalAtualizado().compareTo(limiteRpv) <= 0 ? "RPV" : "PRECATORIO";
        boolean superpreferencia = isSuperpreferencia(request, naturezaCredito, modalidade, calculadoEm);
        Integer idadeBeneficiario = idadeBeneficiario(request.dataNascimentoBeneficiario(), calculadoEm);
        String prioridadePagamento = resolvePrioridadePagamento(modalidade, naturezaCredito, superpreferencia);
        PrecatorioRpvQueuePlanner.QueuePlan queuePlan = queuePlanner.plan(new PrecatorioRpvQueuePlanner.QueuePlanCommand(
                processo,
                modalidade,
                prioridadePagamento,
                naturezaCredito,
                monetaryComputation.totalAtualizado(),
                calculadoEm,
                resolveDataApresentacao(request, calculadoEm),
                idadeBeneficiario,
                request.entidadeDevedoraCodigo(),
                superpreferencia,
                request.regimeEspecial(),
                request.acordoDiretoHabilitado()
        ));
        LocalDate prazoMaximo = "RPV".equals(modalidade) ? calculadoEm.plusMonths(2) : queuePlan.pagamentoEstimadoAte();
        PaymentGovernance paymentGovernance = new PaymentGovernance(
                queuePlan.governanca().publicarListaCronologicaAnonimizada(),
                queuePlan.governanca().publicarPagamentosDoExercicio(),
                queuePlan.governanca().desempatarPorMenorValor(),
                queuePlan.governanca().desempatarPorMaiorIdade(),
                queuePlan.governanca().preservarPosicaoOriginalAposParcelaSuperpreferencial(),
                queuePlan.governanca().monitorarSequestroPorBurlaOuNaoAlocacao(),
                request.regimeEspecial(),
                request.acordoDiretoHabilitado() && request.regimeEspecial()
        );
        PrecatorioRpvCalendarPlanner.AgendaPlan agendaOperacional = calendarPlanner.plan(new PrecatorioRpvCalendarPlanner.AgendaCommand(
                modalidade,
                calculadoEm,
                queuePlan.dataApresentacaoConsiderada(),
                prazoMaximo,
                queuePlan.pagamentoEstimadoAte(),
                superpreferencia,
                request.regimeEspecial(),
                request.acordoDiretoHabilitado(),
                queuePlan.governanca()
        ));
        return new PrecatorioRpvResponse(
                processo.getId(),
                firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()),
                principal,
                monetaryComputation.correcao(),
                monetaryComputation.juros(),
                monetaryComputation.totalAtualizado(),
                modalidade,
                limiteRpv,
                prazoMaximo,
                calculadoEm,
                naturezaCredito,
                enteDevedorTipo,
                prioridadePagamento,
                superpreferencia,
                monetaryComputation.politicaMonetaria(),
                paymentGovernance,
                queuePlan,
                agendaOperacional
        );
    }

    private LocalDate resolveDataApresentacao(PrecatorioRpvRequest request, LocalDate calculadoEm) {
        return request.dataApresentacao() == null ? calculadoEm : request.dataApresentacao();
    }

    private MonetaryComputation computeMonetary(BigDecimal principal,
                                                PrecatorioRpvRequest request,
                                                PrecatorioRpvNaturezaCredito naturezaCredito,
                                                PrecatorioRpvEnteDevedorTipo enteDevedorTipo,
                                                LocalDate calculadoEm) {
        BigDecimal correcaoSolicitada = safe(request.indiceCorrecao());
        BigDecimal jurosSolicitados = safe(request.indiceJuros());
        BigDecimal indiceSelic = safe(request.indiceSelicTeto());
        MonetaryPolicy politica = resolveMonetaryPolicy(naturezaCredito, enteDevedorTipo, calculadoEm, indiceSelic);
        if (politica.selicExclusiva()) {
            BigDecimal correcao = principal.multiply(indiceSelic).setScale(2, RoundingMode.HALF_UP);
            BigDecimal total = principal.add(correcao).setScale(2, RoundingMode.HALF_UP);
            return new MonetaryComputation(correcao, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), total, politica);
        }
        BigDecimal combined = correcaoSolicitada.add(jurosSolicitados);
        if (politica.aplicarTetoSelic() && indiceSelic.signum() > 0 && combined.compareTo(indiceSelic) > 0) {
            BigDecimal correcao = principal.multiply(indiceSelic).setScale(2, RoundingMode.HALF_UP);
            BigDecimal total = principal.add(correcao).setScale(2, RoundingMode.HALF_UP);
            MonetaryPolicy policyWithTeto = politica.withObservacao("indice combinado excedeu a Selic informada e foi substituido pelo teto exclusivo");
            return new MonetaryComputation(correcao, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), total, policyWithTeto);
        }
        BigDecimal correcao = principal.multiply(correcaoSolicitada).setScale(2, RoundingMode.HALF_UP);
        BigDecimal juros = principal.multiply(jurosSolicitados).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = principal.add(correcao).add(juros).setScale(2, RoundingMode.HALF_UP);
        return new MonetaryComputation(correcao, juros, total, politica);
    }

    private MonetaryPolicy resolveMonetaryPolicy(PrecatorioRpvNaturezaCredito naturezaCredito,
                                                                                                  PrecatorioRpvEnteDevedorTipo enteDevedorTipo,
                                                 LocalDate calculadoEm,
                                                 BigDecimal indiceSelic) {
        if (naturezaCredito == PrecatorioRpvNaturezaCredito.TRIBUTARIO) {
            return new MonetaryPolicy(
                    "TRIBUTARIO_SELIC_EXCLUSIVA",
                    "SELIC_EXCLUSIVA",
                    false,
                    true,
                    JUROS_DOIS_POR_CENTO_AO_ANO,
                    indiceSelic,
                    "credito tributario deve observar Selic exclusiva"
            );
        }
        if (enteDevedorTipo.federal() && !calculadoEm.isBefore(MARCO_FEDERAL_EC_136)) {
            return new MonetaryPolicy(
                    "EC_136_2025_FEDERAL",
                    "IPCA_MAIS_2AA_LIMITADO_PELA_SELIC",
                    true,
                    false,
                    JUROS_DOIS_POR_CENTO_AO_ANO,
                    indiceSelic,
                    "precatório federal com data-base a partir de setembro de 2025"
            );
        }
        if (enteDevedorTipo.subnacional() && !calculadoEm.isBefore(MARCO_SUBNACIONAL_EC_136)) {
            return new MonetaryPolicy(
                    "EC_136_2025_SUBNACIONAL",
                    "IPCA_MAIS_2AA_LIMITADO_PELA_SELIC",
                    true,
                    false,
                    JUROS_DOIS_POR_CENTO_AO_ANO,
                    indiceSelic,
                    "precatório estadual, distrital ou municipal com data-base a partir de agosto de 2025"
            );
        }
        return new MonetaryPolicy(
                "CNJ_303_2019_LEGADO",
                "CRITERIOS_LEGADOS_INFORMADOS_NO_PEDIDO",
                false,
                false,
                JUROS_DOIS_POR_CENTO_AO_ANO,
                indiceSelic,
                "conta anterior aos marcos constitucionais recentes; utilizar criterios legados informados"
        );
    }

    private BigDecimal resolvePrincipal(PrecatorioRpvRequest request, Processo processo) {
        if (request.valorPrincipal() != null && request.valorPrincipal().signum() > 0) {
            return request.valorPrincipal().setScale(2, RoundingMode.HALF_UP);
        }
        if (processo.getValorCausa() != null && processo.getValorCausa().signum() > 0) {
            return processo.getValorCausa().setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private PrecatorioRpvNaturezaCredito resolveNaturezaCredito(PrecatorioRpvRequest request) {
        return request.naturezaCredito() == null ? PrecatorioRpvNaturezaCredito.COMUM : request.naturezaCredito();
    }

    private PrecatorioRpvEnteDevedorTipo resolveEnteDevedorTipo(PrecatorioRpvRequest request) {
        return request.enteDevedorTipo() == null ? PrecatorioRpvEnteDevedorTipo.ESTADO : request.enteDevedorTipo();
    }

    private boolean isSuperpreferencia(PrecatorioRpvRequest request,
                                       PrecatorioRpvNaturezaCredito naturezaCredito,
                                       String modalidade,
                                       LocalDate calculadoEm) {
        if (!"PRECATORIO".equals(modalidade) || naturezaCredito != PrecatorioRpvNaturezaCredito.ALIMENTAR) {
            return false;
        }
        Integer idade = idadeBeneficiario(request.dataNascimentoBeneficiario(), calculadoEm);
        return (idade != null && idade >= 60) || request.doencaGrave() || request.pessoaComDeficiencia();
    }

    private Integer idadeBeneficiario(LocalDate nascimento, LocalDate dataReferencia) {
        if (nascimento == null || dataReferencia == null || nascimento.isAfter(dataReferencia)) {
            return null;
        }
        return Period.between(nascimento, dataReferencia).getYears();
    }

    private String resolvePrioridadePagamento(String modalidade,
                                              PrecatorioRpvNaturezaCredito naturezaCredito,
                                              boolean superpreferencia) {
        if ("RPV".equals(modalidade)) {
            return "RPV_EXECUCAO_DIRETA";
        }
        if (superpreferencia) {
            return "SUPERPREFERENCIAL_ALIMENTAR";
        }
        if (naturezaCredito == PrecatorioRpvNaturezaCredito.ALIMENTAR) {
            return "ALIMENTAR_CRONOLOGICA";
        }
        if (naturezaCredito == PrecatorioRpvNaturezaCredito.TRIBUTARIO) {
            return "TRIBUTARIO_CRONOLOGICO";
        }
        return "COMUM_CRONOLOGICA";
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null) {
                String trimmed = value.trim();
                if (!trimmed.isBlank()) {
                    return trimmed;
                }
            }
        }
        return null;
    }

    public record PrecatorioRpvRequest(
            Long processoId,
            BigDecimal valorPrincipal,
            BigDecimal indiceCorrecao,
            BigDecimal indiceJuros,
            BigDecimal indiceSelicTeto,
            BigDecimal limiteRpv,
            PrecatorioRpvNaturezaCredito naturezaCredito,
            PrecatorioRpvEnteDevedorTipo enteDevedorTipo,
            String entidadeDevedoraCodigo,
            LocalDate dataBaseCalculo,
            LocalDate dataApresentacao,
            LocalDate dataNascimentoBeneficiario,
            boolean doencaGrave,
            boolean pessoaComDeficiencia,
            boolean regimeEspecial,
            boolean acordoDiretoHabilitado
    ) {
        public PrecatorioRpvRequest(Long processoId,
                                    BigDecimal valorPrincipal,
                                    BigDecimal indiceCorrecao,
                                    BigDecimal indiceJuros,
                                    BigDecimal limiteRpv) {
            this(processoId, valorPrincipal, indiceCorrecao, indiceJuros, null, limiteRpv, null, null, null, null, null, null, false, false, false, false);
        }
    }

    public record PrecatorioRpvResponse(
            Long processoId,
            String numeroProcesso,
            BigDecimal principal,
            BigDecimal correcao,
            BigDecimal juros,
            BigDecimal totalAtualizado,
            String modalidade,
            BigDecimal limiteRpv,
            LocalDate prazoMaximoPagamento,
            LocalDate calculadoEm,
            PrecatorioRpvNaturezaCredito naturezaCredito,
            PrecatorioRpvEnteDevedorTipo enteDevedorTipo,
            String prioridadePagamento,
            boolean superpreferencia,
            MonetaryPolicy politicaMonetaria,
            PaymentGovernance governancaPagamento,
            PrecatorioRpvQueuePlanner.QueuePlan planoFila,
            PrecatorioRpvCalendarPlanner.AgendaPlan agendaOperacional
    ) {
    }

    public record MonetaryPolicy(
            String regimeNormativo,
            String regraCalculo,
            boolean aplicarTetoSelic,
            boolean selicExclusiva,
            BigDecimal jurosNominaisAnuais,
            BigDecimal indiceSelicInformado,
            String observacao
    ) {
        MonetaryPolicy withObservacao(String novaObservacao) {
            return new MonetaryPolicy(regimeNormativo, regraCalculo, aplicarTetoSelic, selicExclusiva, jurosNominaisAnuais, indiceSelicInformado, novaObservacao);
        }
    }

    public record PaymentGovernance(
            boolean publicarListaCronologicaAnonimizada,
            boolean publicarPagamentosDoExercicio,
            boolean desempatarPorMenorValor,
            boolean desempatarPorMaiorIdade,
            boolean preservarPosicaoOriginalAposParcelaSuperpreferencial,
            boolean monitorarSequestroPorBurlaOuNaoAlocacao,
            boolean regimeEspecial,
            boolean acordoDiretoElegivel
    ) {
    }

    private record MonetaryComputation(
            BigDecimal correcao,
            BigDecimal juros,
            BigDecimal totalAtualizado,
            MonetaryPolicy politicaMonetaria
    ) {
    }
}
