package com.tcc.pjb.backend.service.offline.surface;

import com.tcc.pjb.backend.model.dto.offline.PwaOfflineBundleCreateRequest;
import com.tcc.pjb.backend.model.dto.offline.PwaOfflineBundleSyncRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.offline.PwaOfflineService;
import com.tcc.pjb.backend.service.offline.domain.CriarBundleRequest;
import com.tcc.pjb.backend.service.offline.domain.SincronizarBundleRequest;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import org.springframework.stereotype.Service;

@Service
public class PwaOfflineSurfaceFacadeService {

    private final PwaOfflineService service;
    private final SurfaceProjectionSupport projectionSupport;

    public PwaOfflineSurfaceFacadeService(PwaOfflineService service,
                                          SurfaceProjectionSupport projectionSupport) {
        this.service = service;
        this.projectionSupport = projectionSupport;
    }

    public SurfaceSnapshotResponse criar(PwaOfflineBundleCreateRequest request) {
        return projectionSupport.snapshot(
                "pwa-offline-bundle",
                service.criarBundle(new CriarBundleRequest(request.processoId(), request.escopo(), request.deviceFingerprint()))
        );
    }

    public SurfaceCollectionResponse listar() {
        return projectionSupport.collection("pwa-offline-bundle", service.listarBundlesRecentes());
    }

    public SurfaceSnapshotResponse detalhar(String bundleToken) {
        return projectionSupport.snapshot("pwa-offline-bundle", service.detalhar(bundleToken));
    }

    public SurfaceSnapshotResponse sincronizar(String bundleToken, PwaOfflineBundleSyncRequest request) {
        return projectionSupport.snapshot(
                "pwa-offline-bundle",
                service.sincronizar(
                        bundleToken,
                        new SincronizarBundleRequest(
                                request.acoes(),
                                request.deviceClock(),
                                request.ultimaSincronizacaoConhecida(),
                                request.conflitoResumo()
                        )
                )
        );
    }

    public SurfaceSnapshotResponse governanca(String bundleToken) {
        return projectionSupport.snapshot("pwa-offline-governance", service.governanceStatusView(bundleToken));
    }

    public SurfaceSnapshotResponse timelineConflito(String bundleToken) {
        return projectionSupport.snapshot("pwa-offline-conflict-timeline", service.conflictTimeline(bundleToken));
    }

    public SurfaceSnapshotResponse metricas(String bundleToken) {
        return projectionSupport.snapshot("pwa-offline-metrics", service.metricsView(bundleToken));
    }
}
