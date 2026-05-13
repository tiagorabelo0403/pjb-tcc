package com.tcc.pjb.backend.core.processo.completude.application;

import com.tcc.pjb.backend.core.processo.analytics.application.ProcessoAnalyticsNacionalApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoLegadosApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoLegadosAggregate;
import com.tcc.pjb.backend.core.processo.analytics.domain.ProcessoAnalyticsNacionalAggregate;
import com.tcc.pjb.backend.core.processo.completude.domain.ProcessoFechamentoTotalAggregate;
import com.tcc.pjb.backend.core.processo.hardening.application.ProcessoHardeningFinalApplicationService;
import com.tcc.pjb.backend.core.processo.hardening.domain.ProcessoHardeningAggregate;
import com.tcc.pjb.backend.core.processo.orfandade.application.ProcessoAntiOrfaoApplicationService;
import com.tcc.pjb.backend.core.processo.orfandade.domain.ProcessoAntiOrfaoAggregate;
import com.tcc.pjb.backend.core.processo.plantao.application.ProcessoPlantaoSubstituicaoApplicationService;
import com.tcc.pjb.backend.core.processo.plantao.domain.ProcessoPlantaoSubstituicaoAggregate;
import com.tcc.pjb.backend.core.processo.producao.application.ProcessoOperacaoTransversalApplicationService;
import com.tcc.pjb.backend.core.processo.producao.domain.ProcessoOperacaoTransversalAggregate;
import com.tcc.pjb.backend.core.processo.sinalizacao.application.ProcessoSinalizacaoRegraApplicationService;
import com.tcc.pjb.backend.core.processo.sinalizacao.domain.ProcessoSinalizacaoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoInfraestruturaSoberanaApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoInfraestruturaSoberanaAggregate;
import com.tcc.pjb.backend.core.quality.codebase.application.PjbCodebaseSanityApplicationService;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseSanityAggregate;
import com.tcc.pjb.backend.core.quality.apisurface.domain.PjbApiSurfaceSanityAggregate;
import com.tcc.pjb.backend.core.quality.certificacao.application.PjbCertificacaoOperacionalApplicationService;
import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;
import com.tcc.pjb.backend.core.quality.certificacao.domain.PjbCertificacaoOperacionalAggregate;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaParallelExecutor;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoFechamentoTotalApplicationService {

    private final ProcessoRepository processoRepository;
    private final ProcessoHardeningFinalApplicationService processoHardeningFinalApplicationService;
    private final ProcessoAntiOrfaoApplicationService processoAntiOrfaoApplicationService;
    private final ProcessoSinalizacaoRegraApplicationService processoSinalizacaoRegraApplicationService;
    private final ProcessoPlantaoSubstituicaoApplicationService processoPlantaoSubstituicaoApplicationService;
    private final ProcessoAnalyticsNacionalApplicationService processoAnalyticsNacionalApplicationService;
    private final ProcessoOperacaoTransversalApplicationService processoOperacaoTransversalApplicationService;
    private final ProcessoInfraestruturaSoberanaApplicationService processoInfraestruturaSoberanaApplicationService;
    private final PjbCertificacaoOperacionalApplicationService pjbCertificacaoOperacionalApplicationService;
    private final PjbSubstituicaoLegadosApplicationService pjbSubstituicaoLegadosApplicationService;
    private final PjbCodebaseSanityApplicationService pjbCodebaseSanityApplicationService;
    private final PjbApiSurfaceSanityApplicationService pjbApiSurfaceSanityApplicationService;
    private final ProcessoMalhaParallelExecutor processoMalhaParallelExecutor;

    public ProcessoFechamentoTotalApplicationService(ProcessoRepository processoRepository,
                                                     ProcessoHardeningFinalApplicationService processoHardeningFinalApplicationService,
                                                     ProcessoAntiOrfaoApplicationService processoAntiOrfaoApplicationService,
                                                     ProcessoSinalizacaoRegraApplicationService processoSinalizacaoRegraApplicationService,
                                                     ProcessoPlantaoSubstituicaoApplicationService processoPlantaoSubstituicaoApplicationService,
                                                     ProcessoAnalyticsNacionalApplicationService processoAnalyticsNacionalApplicationService,
                                                     ProcessoOperacaoTransversalApplicationService processoOperacaoTransversalApplicationService,
                                                     ProcessoInfraestruturaSoberanaApplicationService processoInfraestruturaSoberanaApplicationService,
                                                     PjbCertificacaoOperacionalApplicationService pjbCertificacaoOperacionalApplicationService,
                                                     PjbSubstituicaoLegadosApplicationService pjbSubstituicaoLegadosApplicationService,
                                                     PjbCodebaseSanityApplicationService pjbCodebaseSanityApplicationService,
                                                     PjbApiSurfaceSanityApplicationService pjbApiSurfaceSanityApplicationService,
                                                     ProcessoMalhaParallelExecutor processoMalhaParallelExecutor) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.processoHardeningFinalApplicationService = Objects.requireNonNull(processoHardeningFinalApplicationService);
        this.processoAntiOrfaoApplicationService = Objects.requireNonNull(processoAntiOrfaoApplicationService);
        this.processoSinalizacaoRegraApplicationService = Objects.requireNonNull(processoSinalizacaoRegraApplicationService);
        this.processoPlantaoSubstituicaoApplicationService = Objects.requireNonNull(processoPlantaoSubstituicaoApplicationService);
        this.processoAnalyticsNacionalApplicationService = Objects.requireNonNull(processoAnalyticsNacionalApplicationService);
        this.processoOperacaoTransversalApplicationService = Objects.requireNonNull(processoOperacaoTransversalApplicationService);
        this.processoInfraestruturaSoberanaApplicationService = Objects.requireNonNull(processoInfraestruturaSoberanaApplicationService);
        this.pjbCertificacaoOperacionalApplicationService = Objects.requireNonNull(pjbCertificacaoOperacionalApplicationService);
        this.pjbSubstituicaoLegadosApplicationService = Objects.requireNonNull(pjbSubstituicaoLegadosApplicationService);
        this.pjbCodebaseSanityApplicationService = Objects.requireNonNull(pjbCodebaseSanityApplicationService);
        this.pjbApiSurfaceSanityApplicationService = Objects.requireNonNull(pjbApiSurfaceSanityApplicationService);
        this.processoMalhaParallelExecutor = Objects.requireNonNull(processoMalhaParallelExecutor);
    }

    public ProcessoFechamentoTotalAggregate detalhar(Long processoId, String profileCode) {
        Processo processo = processoRepository.findContextoCompletoById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        ProcessoMalhaParallelExecutor.Quarteto<ProcessoHardeningAggregate, ProcessoAntiOrfaoAggregate, ProcessoSinalizacaoAggregate, ProcessoPlantaoSubstituicaoAggregate> loteBase =
                processoMalhaParallelExecutor.executar4(
                        "processo-fechamento-total-base",
                        (java.util.function.Supplier<ProcessoHardeningAggregate>) () -> processoHardeningFinalApplicationService.detalhar(processoId),
                        (java.util.function.Supplier<ProcessoAntiOrfaoAggregate>) () -> processoAntiOrfaoApplicationService.detalhar(processoId),
                        (java.util.function.Supplier<ProcessoSinalizacaoAggregate>) () -> processoSinalizacaoRegraApplicationService.detalhar(processoId, profileCode),
                        (java.util.function.Supplier<ProcessoPlantaoSubstituicaoAggregate>) () -> processoPlantaoSubstituicaoApplicationService.detalhar(processoId)
                );
        ProcessoMalhaParallelExecutor.Quinteto<ProcessoAnalyticsNacionalAggregate, ProcessoOperacaoTransversalAggregate, ProcessoInfraestruturaSoberanaAggregate, PjbCertificacaoOperacionalAggregate, PjbSubstituicaoLegadosAggregate> loteSoberano =
                processoMalhaParallelExecutor.executar5(
                        "processo-fechamento-total-soberano",
                        (java.util.function.Supplier<ProcessoAnalyticsNacionalAggregate>) () -> processoAnalyticsNacionalApplicationService.detalhar(processoId),
                        (java.util.function.Supplier<ProcessoOperacaoTransversalAggregate>) () -> processoOperacaoTransversalApplicationService.detalhar(processoId),
                        (java.util.function.Supplier<ProcessoInfraestruturaSoberanaAggregate>) () -> processoInfraestruturaSoberanaApplicationService.consolidar(processoId),
                        (java.util.function.Supplier<PjbCertificacaoOperacionalAggregate>) () -> pjbCertificacaoOperacionalApplicationService.certificar(processoId),
                        (java.util.function.Supplier<PjbSubstituicaoLegadosAggregate>) () -> pjbSubstituicaoLegadosApplicationService.avaliar(processoId)
                );
        ProcessoHardeningAggregate hardening = loteBase.primeiro();
        ProcessoAntiOrfaoAggregate antiOrfao = loteBase.segundo();
        ProcessoSinalizacaoAggregate sinalizacao = loteBase.terceiro();
        ProcessoPlantaoSubstituicaoAggregate plantaoSubstituicao = loteBase.quarto();
        ProcessoAnalyticsNacionalAggregate analyticsNacional = loteSoberano.primeiro();
        ProcessoOperacaoTransversalAggregate operacaoTransversal = loteSoberano.segundo();
        ProcessoInfraestruturaSoberanaAggregate infraestruturaSoberana = loteSoberano.terceiro();
        PjbCertificacaoOperacionalAggregate certificacaoOperacional = loteSoberano.quarto();
        PjbSubstituicaoLegadosAggregate substituicaoLegados = loteSoberano.quinto();
        PjbCodebaseSanityAggregate codebaseSanity = pjbCodebaseSanityApplicationService.auditar();
        PjbApiSurfaceSanityAggregate apiSurfaceSanity = pjbApiSurfaceSanityApplicationService.auditar();

        long scoreGeral = Math.max(0L, Math.min(100L,
                Math.round((hardening.hardeningScore() * 0.17)
                        + (antiOrfao.coberturaPercentual() * 0.10)
                        + (operacaoTransversal.coberturaGlobal() * 0.10)
                        + (100d - analyticsNacional.riscoSlaGlobal()) * 0.06
                        + (plantaoSubstituicao.alertas().isEmpty() ? 100d : 78d) * 0.03
                        + ("CRITICA".equals(sinalizacao.priorityBand()) ? 65d : 92d) * 0.06
                        + (infraestruturaSoberana.certificacao().cobertura() * 0.13)
                        + (certificacaoOperacional.cobertura() * 0.14)
                        + (substituicaoLegados.scoreGeral() * 0.16)
                        + (codebaseSanity.score() * 0.05))));
        String readiness = hardening.blockingFindings() > 0 || antiOrfao.totalGaps() > 0 || !codebaseSanity.limpo() || !apiSurfaceSanity.limpo() ? "NOT_READY"
                : operacaoTransversal.coberturaGlobal() < 75d || infraestruturaSoberana.certificacao().critico() || certificacaoOperacional.critico() ? "HARDENING_EM_ANDAMENTO"
                : substituicaoLegados.prontoSubstituicaoImediata() && certificacaoOperacional.cobertura() >= 85 && scoreGeral >= 90L ? "READY_FOR_NATIONAL_REPLACEMENT"
                : scoreGeral >= 85L ? "READY_FOR_ROLLOUT"
                : "PILOTO_CONTROLADO";

        LinkedHashSet<String> alertas = new LinkedHashSet<>(hardening.findings().stream().map(item -> item.code() + ':' + item.detail()).toList());
        alertas.addAll(antiOrfao.gaps().stream().map(item -> item.code() + ':' + item.detail()).toList());
        alertas.addAll(sinalizacao.alertas());
        alertas.addAll(plantaoSubstituicao.alertas());
        alertas.addAll(analyticsNacional.alertas());
        alertas.addAll(operacaoTransversal.alertas());
        alertas.addAll(infraestruturaSoberana.fundamentos());
        alertas.addAll(certificacaoOperacional.modulosCriticos());
        alertas.addAll(substituicaoLegados.fundamentos());
        alertas.addAll(codebaseSanity.issues().stream().map(item -> item.codigo() + ":" + item.detalhe()).toList());
        alertas.addAll(apiSurfaceSanity.issues().stream().map(item -> item.codigo() + ":" + item.alvo()).toList());

        LinkedHashSet<String> plano = new LinkedHashSet<>(hardening.correctivePlan());
        plano.addAll(antiOrfao.proximasAcoes());
        plano.addAll(operacaoTransversal.proximasAcoes());
        plano.addAll(infraestruturaSoberana.fundamentos());
        plano.addAll(certificacaoOperacional.modulosCriticos());
        plano.addAll(substituicaoLegados.fundamentos());
        plano.addAll(codebaseSanity.issues().stream().map(item -> item.codigo() + " => " + item.detalhe()).toList());
        plano.addAll(apiSurfaceSanity.issues().stream().map(item -> item.codigo() + " => " + item.alvo()).toList());
        plano.add("MANTER_BLOCOS_NOVOS_SOB_SCANNER_ANTI_ORFAO");
        plano.add("USAR_FECHAMENTO_TOTAL_COMO_GATE_DE_ROLLOUT");

        return new ProcessoFechamentoTotalAggregate(
                processoId,
                processo.getNumeroProcesso(),
                readiness,
                scoreGeral,
                hardening,
                antiOrfao,
                sinalizacao,
                plantaoSubstituicao,
                analyticsNacional,
                operacaoTransversal,
                infraestruturaSoberana,
                certificacaoOperacional,
                substituicaoLegados,
                codebaseSanity,
                List.copyOf(alertas),
                List.copyOf(plano),
                Instant.now()
        );
    }
}
