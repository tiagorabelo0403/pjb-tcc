package com.tcc.pjb.backend.core.processo.painel.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class ProcessoPainelBndtApplicationServiceTest {

    @Test
    void marcaPainelTrabalhistaComoAplicavel() {
        var unificadoService = mock(com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService.class);
        var timelineService = mock(com.tcc.pjb.backend.core.processo.timeline.application.ProcessoTimelineApplicationService.class);
        var analyticsService = mock(com.tcc.pjb.backend.core.processo.analytics.application.ProcessoAnalyticsNacionalApplicationService.class);
        var operacaoService = mock(com.tcc.pjb.backend.core.processo.producao.application.ProcessoOperacaoTransversalApplicationService.class);
        var fonteService = mock(ProcessoPainelFonteOficialApplicationService.class);
        ProcessoPainelBndtApplicationService service = new ProcessoPainelBndtApplicationService(unificadoService, timelineService, analyticsService, operacaoService, fonteService);

        when(unificadoService.detalhar(20L)).thenReturn(PainelTestData.unificado(20L, "TRABALHISTA", "TRT7"));
        when(timelineService.detalhar(20L)).thenReturn(PainelTestData.timeline(20L, 0, 1));
        when(analyticsService.detalhar(20L)).thenReturn(PainelTestData.analytics(20L, 45d));
        when(operacaoService.detalhar(20L)).thenReturn(PainelTestData.operacao(20L, "READY", 88d));
        when(fonteService.detalhar(20L)).thenReturn(PainelTestData.fontes(20L, "TRABALHISTA"));

        var aggregate = service.detalhar(20L);

        assertThat(aggregate.aplicavel()).isTrue();
        assertThat(aggregate.status()).isEqualTo("PRONTA");
        assertThat(aggregate.fonteOficial()).contains("BNDT");
    }
}
