package com.tcc.pjb.backend.core.processo.completude;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoLegadosApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoLegadosAggregate;
import com.tcc.pjb.backend.core.processo.analytics.application.ProcessoAnalyticsNacionalApplicationService;
import com.tcc.pjb.backend.core.processo.analytics.domain.ProcessoAnalyticsNacionalAggregate;
import com.tcc.pjb.backend.core.processo.busca.domain.ProcessoAnalyticsAggregate;
import com.tcc.pjb.backend.core.processo.completude.application.ProcessoFechamentoTotalApplicationService;
import com.tcc.pjb.backend.core.processo.hardening.application.ProcessoHardeningFinalApplicationService;
import com.tcc.pjb.backend.core.processo.hardening.domain.ProcessoHardeningAggregate;
import com.tcc.pjb.backend.core.processo.orfandade.application.ProcessoAntiOrfaoApplicationService;
import com.tcc.pjb.backend.core.processo.orfandade.domain.ProcessoAntiOrfaoAggregate;
import com.tcc.pjb.backend.core.processo.plantao.application.ProcessoPlantaoSubstituicaoApplicationService;
import com.tcc.pjb.backend.core.processo.plantao.domain.ProcessoPlantaoSubstituicaoAggregate;
import com.tcc.pjb.backend.core.processo.producao.application.ProcessoOperacaoTransversalApplicationService;
import com.tcc.pjb.backend.core.processo.producao.domain.ProcessoOperacaoTransversalAggregate;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaParallelExecutor;
import com.tcc.pjb.backend.core.processo.sinalizacao.application.ProcessoSinalizacaoRegraApplicationService;
import com.tcc.pjb.backend.core.processo.sinalizacao.domain.ProcessoSinalizacaoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoInfraestruturaSoberanaApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoInfraestruturaSoberanaAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity;
import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;
import com.tcc.pjb.backend.core.quality.apisurface.domain.PjbApiSurfaceSanityAggregate;
import com.tcc.pjb.backend.core.quality.certificacao.application.PjbCertificacaoOperacionalApplicationService;
import com.tcc.pjb.backend.core.quality.certificacao.domain.PjbCertificacaoOperacionalAggregate;
import com.tcc.pjb.backend.core.quality.codebase.application.PjbCodebaseSanityApplicationService;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseSanityAggregate;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProcessoFechamentoTotalApplicationServiceTest {

    @Test
    void deveConsolidarFechamentoFinalSemOrfandade() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        ProcessoHardeningFinalApplicationService hardening = mock(ProcessoHardeningFinalApplicationService.class);
        ProcessoAntiOrfaoApplicationService antiOrfao = mock(ProcessoAntiOrfaoApplicationService.class);
        ProcessoSinalizacaoRegraApplicationService sinalizacao = mock(ProcessoSinalizacaoRegraApplicationService.class);
        ProcessoPlantaoSubstituicaoApplicationService plantao = mock(ProcessoPlantaoSubstituicaoApplicationService.class);
        ProcessoAnalyticsNacionalApplicationService analytics = mock(ProcessoAnalyticsNacionalApplicationService.class);
        ProcessoOperacaoTransversalApplicationService operacao = mock(ProcessoOperacaoTransversalApplicationService.class);
        ProcessoInfraestruturaSoberanaApplicationService infraestrutura = mock(ProcessoInfraestruturaSoberanaApplicationService.class);
        PjbCertificacaoOperacionalApplicationService certificacao = mock(PjbCertificacaoOperacionalApplicationService.class);
        PjbSubstituicaoLegadosApplicationService substituicaoLegados = mock(PjbSubstituicaoLegadosApplicationService.class);
        PjbCodebaseSanityApplicationService codebaseSanity = mock(PjbCodebaseSanityApplicationService.class);
        PjbApiSurfaceSanityApplicationService apiSurfaceSanity = mock(PjbApiSurfaceSanityApplicationService.class);
        ProcessoMalhaParallelExecutor parallelExecutor = mock(ProcessoMalhaParallelExecutor.class);

        when(processoRepository.findContextoCompletoById(77L)).thenReturn(Optional.of(Processo.builder().id(77L).numeroProcesso("0077").build()));
        when(hardening.detalhar(77L)).thenReturn(new ProcessoHardeningAggregate(new ProcessoUnificadoIdentity(77L, "0077", "0077", "TJCE", "CE", "FORTALEZA", "1VC", "Classe", "Assunto", "Autor", "Réu", List.of()), "READY_FOR_PILOT", 90L, 0L, 0L, List.of(), List.of(), List.of("A"), List.of(), Instant.now()));
        when(antiOrfao.detalhar(77L)).thenReturn(new ProcessoAntiOrfaoAggregate(77L, "0077", 100L, 10L, 10L, 0L, List.of(), List.of(), List.of("B"), Instant.now()));
        when(sinalizacao.detalhar(77L, null)).thenReturn(new ProcessoSinalizacaoAggregate(77L, "0077", "slate", "blue", "NORMAL", List.of(), List.of(), List.of(), Instant.now()));
        when(plantao.detalhar(77L)).thenReturn(new ProcessoPlantaoSubstituicaoAggregate(77L, "0077", "1VC", "ROTINA", false, false, false, false, "UNIDADE_TITULAR", List.of(), List.of(), List.of("UNIDADE_TITULAR"), List.of(), List.of(), Instant.now()));
        when(analytics.detalhar(77L)).thenReturn(new ProcessoAnalyticsNacionalAggregate(77L, Map.of(), new ProcessoAnalyticsAggregate(Map.of(), 10L, 8L, 12d, 3d, 4d, 2d, List.of(), List.of(), Instant.now()), 10d, 2d, 1L, 8d, List.of(), List.of(), List.of(), Instant.now()));
        when(operacao.detalhar(77L)).thenReturn(new ProcessoOperacaoTransversalAggregate(77L, "0077", "READY", 90d, 20d, List.of(), List.of(), List.of("C"), Instant.now()));
        when(infraestrutura.consolidar(77L)).thenReturn(new ProcessoInfraestruturaSoberanaAggregate(
                77L,
                "0077",
                mock(com.tcc.pjb.backend.core.governance.fonte.domain.ProcessoFonteSoberanaAggregate.class),
                mock(com.tcc.pjb.backend.core.processo.cumprimento.domain.ProcessoCumprimentoOperacionalAggregate.class),
                mock(com.tcc.pjb.backend.core.processo.cooperacao.domain.ProcessoCooperacaoInstitucionalAggregate.class),
                new PjbCertificacaoOperacionalAggregate(77L, "0077", List.of(), 95, false, List.of(), Instant.now()),
                mock(com.tcc.pjb.backend.core.processo.gemeo.domain.ProcessoGemeoDigitalAggregate.class),
                List.of("INFRA_OK"),
                Instant.now()));
        when(certificacao.certificar(77L)).thenReturn(new PjbCertificacaoOperacionalAggregate(77L, "0077", List.of(), 95, false, List.of(), Instant.now()));
        when(substituicaoLegados.avaliar(77L)).thenReturn(new PjbSubstituicaoLegadosAggregate(77L, "0077", List.of(), List.of(), 90, true, "ok", List.of("LEGADO_OK"), Instant.now()));
        when(codebaseSanity.auditar()).thenReturn(new PjbCodebaseSanityAggregate(true, true, 100, 0, 0, 0, List.of(), List.of("src/main/java"), List.of(), Instant.now()));
        when(apiSurfaceSanity.auditar()).thenReturn(new PjbApiSurfaceSanityAggregate(true, true, 10, 10, 0, 0, 0, List.of(), Instant.now()));

        when(parallelExecutor.executar4(any(), any(), any(), any(), any())).thenAnswer(invocation -> new ProcessoMalhaParallelExecutor.Quarteto<>(
                ((java.util.function.Supplier<ProcessoHardeningAggregate>) invocation.getArgument(1)).get(),
                ((java.util.function.Supplier<ProcessoAntiOrfaoAggregate>) invocation.getArgument(2)).get(),
                ((java.util.function.Supplier<ProcessoSinalizacaoAggregate>) invocation.getArgument(3)).get(),
                ((java.util.function.Supplier<ProcessoPlantaoSubstituicaoAggregate>) invocation.getArgument(4)).get()
        ));
        when(parallelExecutor.executar5(any(), any(), any(), any(), any(), any())).thenAnswer(invocation -> new ProcessoMalhaParallelExecutor.Quinteto<>(
                ((java.util.function.Supplier<ProcessoAnalyticsNacionalAggregate>) invocation.getArgument(1)).get(),
                ((java.util.function.Supplier<ProcessoOperacaoTransversalAggregate>) invocation.getArgument(2)).get(),
                ((java.util.function.Supplier<ProcessoInfraestruturaSoberanaAggregate>) invocation.getArgument(3)).get(),
                ((java.util.function.Supplier<PjbCertificacaoOperacionalAggregate>) invocation.getArgument(4)).get(),
                ((java.util.function.Supplier<PjbSubstituicaoLegadosAggregate>) invocation.getArgument(5)).get()
        ));

        ProcessoFechamentoTotalApplicationService service = new ProcessoFechamentoTotalApplicationService(
                processoRepository,
                hardening,
                antiOrfao,
                sinalizacao,
                plantao,
                analytics,
                operacao,
                infraestrutura,
                certificacao,
                substituicaoLegados,
                codebaseSanity,
                apiSurfaceSanity,
                parallelExecutor
        );
        var aggregate = service.detalhar(77L, null);

        assertThat(aggregate.scoreGeral()).isPositive();
        assertThat(aggregate.plano()).isNotEmpty();
        assertThat(aggregate.readiness()).isNotBlank();
    }
}
