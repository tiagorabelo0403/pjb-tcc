package com.tcc.pjb.backend.service.ajuizamento.federal.surface;

import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.ajuizamento.federal.FederalismoRedistribuicaoService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class FederalismoRedistribuicaoSurfaceFacadeService {

    private final FederalismoRedistribuicaoService service;
    private final SurfaceProjectionSupport projectionSupport;

    public FederalismoRedistribuicaoSurfaceFacadeService(FederalismoRedistribuicaoService service,
                                                         SurfaceProjectionSupport projectionSupport) {
        this.service = Objects.requireNonNull(service);
        this.projectionSupport = Objects.requireNonNull(projectionSupport);
    }

    public SurfaceSnapshotResponse sugestoes(double indiceCritico) {
        return projectionSupport.snapshot("admin.federalismo.redistribuicao", service.sugerir(indiceCritico));
    }
}
