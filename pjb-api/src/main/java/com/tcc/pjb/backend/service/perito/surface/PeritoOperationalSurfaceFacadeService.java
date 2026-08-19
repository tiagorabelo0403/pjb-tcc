package com.tcc.pjb.backend.service.perito.surface;

import com.tcc.pjb.backend.model.dto.profile.operational.PeritoHonorariosRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.PeritoLaudoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.PeritoQuesitosRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.perito.PeritoOperacionalEnhancedService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import org.springframework.stereotype.Service;

@Service
public class PeritoOperationalSurfaceFacadeService {

    private final PeritoOperacionalEnhancedService service;
    private final SurfaceProjectionSupport projectionSupport;

    public PeritoOperationalSurfaceFacadeService(PeritoOperacionalEnhancedService service,
                                                 SurfaceProjectionSupport projectionSupport) {
        this.service = service;
        this.projectionSupport = projectionSupport;
    }

    public SurfaceSnapshotResponse snapshot() {
        return projectionSupport.snapshot("perito-operacional", service.bootstrapPainel());
    }

    public SurfaceActionResponse aceitarNomeacao(Long processoId) {
        return projectionSupport.action("perito-operacional", "aceitar-nomeacao", processoId, service.aceitarNomeacao(processoId));
    }

    public SurfaceActionResponse apresentarLaudo(Long processoId, PeritoLaudoRequest request) {
        return projectionSupport.action("perito-operacional", "apresentar-laudo", processoId,
                service.apresentarLaudo(processoId, request));
    }

    public SurfaceActionResponse responderQuesitos(Long processoId, PeritoQuesitosRequest request) {
        return projectionSupport.action("perito-operacional", "responder-quesitos", processoId,
                service.responderQuesitos(processoId, request.respostas()));
    }

    public SurfaceActionResponse solicitarHonorarios(Long processoId, PeritoHonorariosRequest request) {
        return projectionSupport.action("perito-operacional", "solicitar-honorarios", processoId,
                service.solicitarHonorarios(processoId, request.valor(), request.justificativa()));
    }

    public SurfaceCollectionResponse listarHonorarios() {
        return projectionSupport.collection("perito-operacional-honorarios", service.listarHonorarios());
    }
}
