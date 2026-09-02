package com.tcc.pjb.backend.service.processual.surface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.documental.application.ProcessoDocumentoApplicationService;
import com.tcc.pjb.backend.core.processo.documental.domain.ProcessoDocumentoAggregate;
import com.tcc.pjb.backend.core.processo.timeline.application.ProcessoTimelineApplicationService;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineAggregate;
import com.tcc.pjb.backend.core.processo.trabalho.application.ProcessoTrabalhoApplicationService;
import com.tcc.pjb.backend.core.processo.trabalho.domain.ProcessoTrabalhoAggregate;
import org.junit.jupiter.api.Test;

class ProcessoSurfaceArtefatoOrchestratorTest {

    private final ProcessoTrabalhoApplicationService trabalho = mock(ProcessoTrabalhoApplicationService.class);
    private final ProcessoDocumentoApplicationService documento = mock(ProcessoDocumentoApplicationService.class);
    private final ProcessoTimelineApplicationService timeline = mock(ProcessoTimelineApplicationService.class);
    private final ProcessoSurfaceArtefatoOrchestrator orchestrator = new ProcessoSurfaceArtefatoOrchestrator(trabalho, documento, timeline);

    @Test
    void metodos3Delegam() {
        var w = mock(ProcessoTrabalhoAggregate.class);
        var d = mock(ProcessoDocumentoAggregate.class);
        var t = mock(ProcessoTimelineAggregate.class);
        when(trabalho.detalhar(1L)).thenReturn(w);
        when(documento.detalhar(1L)).thenReturn(d);
        when(timeline.detalhar(1L)).thenReturn(t);
        assertThat(orchestrator.workstream(1L)).isSameAs(w);
        assertThat(orchestrator.documentos(1L)).isSameAs(d);
        assertThat(orchestrator.timeline(1L)).isSameAs(t);
    }
}
