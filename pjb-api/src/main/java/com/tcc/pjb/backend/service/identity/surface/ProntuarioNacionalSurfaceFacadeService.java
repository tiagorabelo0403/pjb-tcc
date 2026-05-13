package com.tcc.pjb.backend.service.identity.surface;

import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.service.identity.ProntuarioNacionalService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import org.springframework.stereotype.Service;

@Service
public class ProntuarioNacionalSurfaceFacadeService {

    private final ProntuarioNacionalService service;
    private final SurfaceProjectionSupport projectionSupport;

    public ProntuarioNacionalSurfaceFacadeService(ProntuarioNacionalService service,
                                                  SurfaceProjectionSupport projectionSupport) {
        this.service = service;
        this.projectionSupport = projectionSupport;
    }

    public SurfaceSnapshotResponse consultarPorDocumento(String documento) {
        return projectionSupport.snapshot("prontuario-nacional", service.consultarPorDocumento(documento));
    }

    public SurfaceSnapshotResponse detectarConflitos(String documentoAutor, String documentoReu, RamoDireito ramoDireito) {
        return projectionSupport.snapshot("prontuario-nacional", service.detectarLitispendenciaOuCoisaJulgada(documentoAutor, documentoReu, ramoDireito));
    }
}
