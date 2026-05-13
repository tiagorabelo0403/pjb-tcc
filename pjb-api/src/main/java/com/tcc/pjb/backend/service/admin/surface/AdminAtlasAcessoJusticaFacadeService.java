package com.tcc.pjb.backend.service.admin.surface;

import com.tcc.pjb.backend.model.dto.atlas.AtlasCelulaUpsertRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import com.tcc.pjb.backend.inovacao.atlas.AtlasAcessoJusticaService;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminAtlasAcessoJusticaFacadeService {

    private final AtlasAcessoJusticaService service;
    private final SurfaceProjectionSupport projectionSupport;

    public AdminAtlasAcessoJusticaFacadeService(AtlasAcessoJusticaService service,
                                                SurfaceProjectionSupport projectionSupport) {
        this.service = service;
        this.projectionSupport = projectionSupport;
    }

    public SurfaceSnapshotResponse upsert(AtlasCelulaUpsertRequest request) {
        return projectionSupport.snapshot("admin-atlas-acesso-justica", service.registrarCelula(request));
    }

    public SurfaceSnapshotResponse syncIbge() {
        return projectionSupport.snapshot("admin-atlas-acesso-justica", Map.of("sincronizados", service.sincronizarBaseIbge()));
    }
}
