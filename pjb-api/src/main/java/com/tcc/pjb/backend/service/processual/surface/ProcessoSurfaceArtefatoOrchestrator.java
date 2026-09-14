package com.tcc.pjb.backend.service.processual.surface;

import com.tcc.pjb.backend.core.processo.documental.application.ProcessoDocumentoApplicationService;
import com.tcc.pjb.backend.core.processo.documental.domain.ProcessoDocumentoAggregate;
import com.tcc.pjb.backend.core.processo.timeline.application.ProcessoTimelineApplicationService;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineAggregate;
import com.tcc.pjb.backend.core.processo.trabalho.application.ProcessoTrabalhoApplicationService;
import com.tcc.pjb.backend.core.processo.trabalho.domain.ProcessoTrabalhoAggregate;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Extraído (F6) de ProcessoSurfaceFacadeService: artefatos e cronologia --
 * workstream (fluxo de trabalho + workitems), acervo documental (lotes,
 * minutas, assinaturas) e timeline (eixos de evolução).
 */
@Service
public class ProcessoSurfaceArtefatoOrchestrator {

    private final ProcessoTrabalhoApplicationService processoTrabalhoApplicationService;
    private final ProcessoDocumentoApplicationService processoDocumentoApplicationService;
    private final ProcessoTimelineApplicationService processoTimelineApplicationService;

    public ProcessoSurfaceArtefatoOrchestrator(ProcessoTrabalhoApplicationService processoTrabalhoApplicationService,
                                                ProcessoDocumentoApplicationService processoDocumentoApplicationService,
                                                ProcessoTimelineApplicationService processoTimelineApplicationService) {
        this.processoTrabalhoApplicationService = Objects.requireNonNull(processoTrabalhoApplicationService);
        this.processoDocumentoApplicationService = Objects.requireNonNull(processoDocumentoApplicationService);
        this.processoTimelineApplicationService = Objects.requireNonNull(processoTimelineApplicationService);
    }

    public ProcessoTrabalhoAggregate workstream(Long processoId) {
        return processoTrabalhoApplicationService.detalhar(processoId);
    }

    public ProcessoDocumentoAggregate documentos(Long processoId) {
        return processoDocumentoApplicationService.detalhar(processoId);
    }

    public ProcessoTimelineAggregate timeline(Long processoId) {
        return processoTimelineApplicationService.detalhar(processoId);
    }
}
