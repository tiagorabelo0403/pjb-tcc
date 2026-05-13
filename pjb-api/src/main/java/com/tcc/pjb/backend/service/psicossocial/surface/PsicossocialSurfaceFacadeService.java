package com.tcc.pjb.backend.service.psicossocial.surface;

import com.tcc.pjb.backend.model.dto.psicossocial.PsicossocialAnaliseLaudoRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.psicossocial.PsicossocialRiskService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import org.springframework.stereotype.Service;

@Service
public class PsicossocialSurfaceFacadeService {

    private final PsicossocialRiskService service;
    private final SurfaceProjectionSupport projectionSupport;

    public PsicossocialSurfaceFacadeService(PsicossocialRiskService service,
                                            SurfaceProjectionSupport projectionSupport) {
        this.service = service;
        this.projectionSupport = projectionSupport;
    }

    public SurfaceSnapshotResponse analisar(PsicossocialAnaliseLaudoRequest request) {
        return projectionSupport.snapshot("psicossocial-risco", service.analisar(new PsicossocialRiskService.AnaliseLaudoRequest(
                request.processoId(),
                request.textoLaudo()
        )));
    }
}
