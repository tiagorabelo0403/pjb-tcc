package com.tcc.pjb.backend.service.jurisprudencia.surface;

import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.jurisprudencia.RadarNacionalJurisprudenciaService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import org.springframework.stereotype.Service;

@Service
public class RadarJurisprudenciaSurfaceFacadeService {

    private final RadarNacionalJurisprudenciaService service;
    private final SurfaceProjectionSupport projectionSupport;

    public RadarJurisprudenciaSurfaceFacadeService(RadarNacionalJurisprudenciaService service,
                                                   SurfaceProjectionSupport projectionSupport) {
        this.service = service;
        this.projectionSupport = projectionSupport;
    }

    public SurfaceSnapshotResponse analisar(Long processoId) {
        return projectionSupport.snapshot("radar-jurisprudencia", service.analisar(processoId));
    }
}
