package com.tcc.pjb.backend.core.processo.painel.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.service.financeiro.previdenciario.CnisAnalyzer;
import org.junit.jupiter.api.Test;

class ProcessoPainelPrevidenciarioTrilhoApplicationServiceTest {

    @Test
    void fechaTrilhoPrevidenciarioComFontesOperacionais() {
        var unificadoService = mock(com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService.class);
        var timelineService = mock(com.tcc.pjb.backend.core.processo.timeline.application.ProcessoTimelineApplicationService.class);
        var analyticsService = mock(com.tcc.pjb.backend.core.processo.analytics.application.ProcessoAnalyticsNacionalApplicationService.class);
        var operacaoService = mock(com.tcc.pjb.backend.core.processo.producao.application.ProcessoOperacaoTransversalApplicationService.class);
        var execucaoService = mock(com.tcc.pjb.backend.core.processo.execucao.application.ProcessoExecucaoApplicationService.class);
        var fonteService = mock(ProcessoPainelFonteOficialApplicationService.class);
        ProcessoPainelPrevidenciarioTrilhoApplicationService service = new ProcessoPainelPrevidenciarioTrilhoApplicationService(
                unificadoService,
                timelineService,
                analyticsService,
                operacaoService,
                execucaoService,
                fonteService,
                new CnisAnalyzer()
        );

        when(unificadoService.detalhar(30L)).thenReturn(PainelTestData.unificado(30L, "PREVIDENCIARIO", "TRF5"));
        when(timelineService.detalhar(30L)).thenReturn(PainelTestData.timeline(30L, 1, 2));
        when(analyticsService.detalhar(30L)).thenReturn(PainelTestData.analytics(30L, 55d));
        when(operacaoService.detalhar(30L)).thenReturn(PainelTestData.operacao(30L, "READY", 82d));
        when(execucaoService.detalhar(30L)).thenReturn(PainelTestData.execucao(30L, 1));
        when(fonteService.detalhar(30L)).thenReturn(PainelTestData.fontes(30L, "PREVIDENCIARIO"));

        var aggregate = service.detalhar(30L);

        assertThat(aggregate.aplicavel()).isTrue();
        assertThat(aggregate.fontes()).extracting("code").contains("CNIS", "SABI", "PLENUS");
        assertThat(aggregate.pagamentoStatus()).isEqualTo("EM_ESTEIRA_RPV_PRECATORIO");
    }
}
