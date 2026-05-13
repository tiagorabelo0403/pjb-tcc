package com.tcc.pjb.backend.service.admin.surface;

import com.tcc.pjb.backend.judicial.connectors.application.JudicialConnectorHubService;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import java.time.Duration;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class AdminJudicialConnectorHubSurfaceFacadeService {

    private final JudicialConnectorHubService hubService;
    private final SurfaceProjectionSupport projectionSupport;

    public AdminJudicialConnectorHubSurfaceFacadeService(JudicialConnectorHubService hubService,
                                                         SurfaceProjectionSupport projectionSupport) {
        this.hubService = Objects.requireNonNull(hubService);
        this.projectionSupport = Objects.requireNonNull(projectionSupport);
    }

    public SurfaceSnapshotResponse national(Long horizonSeconds) {
        return projectionSupport.snapshot("admin.judicial.connector.hub.national", hubService.nationalReport(resolveHorizon(horizonSeconds)).toMap());
    }

    public SurfaceSnapshotResponse tribunal(String tribunalCodigo, Long horizonSeconds) {
        return projectionSupport.snapshot("admin.judicial.connector.hub.tribunal", hubService.tribunalReport(tribunalCodigo, resolveHorizon(horizonSeconds)).toMap());
    }

    public SurfaceSnapshotResponse structure() {
        return projectionSupport.snapshot("admin.judicial.connector.hub.structure", hubService.structureReport().toMap());
    }

    private Duration resolveHorizon(Long horizonSeconds) {
        return horizonSeconds == null ? Duration.ofHours(24) : Duration.ofSeconds(Math.max(60L, horizonSeconds));
    }
}
