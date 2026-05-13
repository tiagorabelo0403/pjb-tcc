package com.tcc.pjb.backend.service.lgpd.surface;

import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import com.tcc.pjb.backend.core.lgpd.LgpdProcessualSensibilityEngine;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LgpdProcessualSurfaceFacadeService {

    private final LgpdProcessualSensibilityEngine engine;
    private final SurfaceProjectionSupport projectionSupport;

    public LgpdProcessualSurfaceFacadeService(LgpdProcessualSensibilityEngine engine,
                                              SurfaceProjectionSupport projectionSupport) {
        this.engine = Objects.requireNonNull(engine);
        this.projectionSupport = Objects.requireNonNull(projectionSupport);
    }

    public SurfaceSnapshotResponse classificar(Long processoId) {
        return projectionSupport.snapshot("lgpd.processual.classificacao", engine.classificar(processoId));
    }

    public SurfaceSnapshotResponse politicaRetencao(Long processoId) {
        return projectionSupport.snapshot("lgpd.processual.retencao", engine.politicaRetencao(processoId));
    }

    public SurfaceSnapshotResponse auditarAcessos(Long processoId) {
        return projectionSupport.snapshot("lgpd.processual.acessos", engine.auditarAcessosDados(processoId));
    }

    public SurfaceSnapshotResponse relatorioImpacto(Long processoId) {
        return projectionSupport.snapshot("lgpd.processual.relatorio-impacto", engine.gerarRelatorioImpacto(processoId));
    }
}
