package com.tcc.pjb.backend.service.extrajudicial.surface;

import com.tcc.pjb.backend.model.dto.extrajudicial.EscrituraLavraturaRequest;
import com.tcc.pjb.backend.model.dto.extrajudicial.EscrituraVinculacaoRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.extrajudicial.EscrituraExtrajudicialService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import org.springframework.stereotype.Service;

@Service
public class CartorioExtrajudicialSurfaceFacadeService {

    private final EscrituraExtrajudicialService service;
    private final SurfaceProjectionSupport projectionSupport;

    public CartorioExtrajudicialSurfaceFacadeService(EscrituraExtrajudicialService service,
                                                     SurfaceProjectionSupport projectionSupport) {
        this.service = service;
        this.projectionSupport = projectionSupport;
    }

    public SurfaceCollectionResponse listar(Long processoId) {
        return projectionSupport.collection("extrajudicial-escritura", processoId == null ? service.minhasEscrituras() : service.listarPorProcesso(processoId));
    }

    public SurfaceSnapshotResponse lavrar(EscrituraLavraturaRequest request) {
        return projectionSupport.snapshot("extrajudicial-escritura", service.lavrar(new EscrituraExtrajudicialService.LavraturaRequest(
                request.tipo(), request.atoResumo(), request.partesResumo(), request.bensResumo(), request.valorDeclarado()
        )));
    }

    public SurfaceSnapshotResponse vincular(Long escrituraId, Long processoId, EscrituraVinculacaoRequest request) {
        return projectionSupport.snapshot("extrajudicial-escritura", service.vincularProcesso(escrituraId, processoId,
                new EscrituraExtrajudicialService.VinculacaoProcessoRequest(request.observacaoVinculacao())));
    }
}
