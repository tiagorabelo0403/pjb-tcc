package com.tcc.pjb.backend.service.ministro.surface;

import com.tcc.pjb.backend.model.dto.ministro.TemaRecursoRepetitivoAfetarRequest;
import com.tcc.pjb.backend.model.dto.ministro.TemaRecursoRepetitivoJulgarRequest;
import com.tcc.pjb.backend.model.dto.ministro.TemaRecursoRepetitivoRelacionarRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.ministro.TemaRecursoRepetitivoService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import org.springframework.stereotype.Service;

@Service
public class TemaRecursoRepetitivoSurfaceFacadeService {

    private final TemaRecursoRepetitivoService service;
    private final SurfaceProjectionSupport projectionSupport;

    public TemaRecursoRepetitivoSurfaceFacadeService(TemaRecursoRepetitivoService service,
                                                     SurfaceProjectionSupport projectionSupport) {
        this.service = service;
        this.projectionSupport = projectionSupport;
    }

    public SurfaceCollectionResponse listar(String status) {
        return projectionSupport.collection("tema-recurso-repetitivo", service.listar(status));
    }

    public SurfaceSnapshotResponse afetar(Long processoId, TemaRecursoRepetitivoAfetarRequest request) {
        return projectionSupport.snapshot("tema-recurso-repetitivo", service.afetar(processoId,
                new TemaRecursoRepetitivoService.AfetarTemaRequest(
                        request.codigo(),
                        request.ementa(),
                        request.fundamentosResumo(),
                        request.criterioAfetacao()
                )));
    }

    public SurfaceSnapshotResponse sobrestar(Long temaId, TemaRecursoRepetitivoRelacionarRequest request) {
        return projectionSupport.snapshot("tema-recurso-repetitivo", service.sobrestar(temaId,
                new TemaRecursoRepetitivoService.RelacionarProcessosRequest(request.processoIds())));
    }

    public SurfaceSnapshotResponse julgar(Long temaId, TemaRecursoRepetitivoJulgarRequest request) {
        return projectionSupport.snapshot("tema-recurso-repetitivo", service.julgar(temaId,
                new TemaRecursoRepetitivoService.JulgarTemaRequest(
                        request.ementa(),
                        request.teseFirmada(),
                        request.fundamentosResumo()
                )));
    }

    public SurfaceSnapshotResponse aplicar(Long temaId, TemaRecursoRepetitivoRelacionarRequest request) {
        return projectionSupport.snapshot("tema-recurso-repetitivo", service.aplicarResultado(temaId,
                new TemaRecursoRepetitivoService.RelacionarProcessosRequest(request.processoIds())));
    }
}
