package com.tcc.pjb.backend.controller.offline;

import com.tcc.pjb.backend.model.dto.offline.PwaOfflineBundleCreateRequest;
import com.tcc.pjb.backend.model.dto.offline.PwaOfflineBundleSyncRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.offline.surface.PwaOfflineSurfaceFacadeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pwa/offline")
public class PwaOfflineController {

    private final PwaOfflineSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public PwaOfflineController(PwaOfflineSurfaceFacadeService facadeService,
                                CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/bundles")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SurfaceSnapshotResponse> criar(@Valid @RequestBody PwaOfflineBundleCreateRequest request,
                                                         Authentication authentication) {
        enforce(authentication, "pwa_offline_bundle_criar");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.criar(request));
    }

    @GetMapping("/bundles")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SurfaceCollectionResponse> listar(Authentication authentication) {
        enforce(authentication, "pwa_offline_bundle_listar");
        return ResponseEntity.ok(facadeService.listar());
    }

    @GetMapping("/bundles/{bundleToken}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SurfaceSnapshotResponse> detalhar(@PathVariable String bundleToken,
                                                            Authentication authentication) {
        enforce(authentication, "pwa_offline_bundle_detalhar");
        return ResponseEntity.ok(facadeService.detalhar(bundleToken));
    }

    @PostMapping("/bundles/{bundleToken}/sync")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SurfaceSnapshotResponse> sincronizar(@PathVariable String bundleToken,
                                                               @Valid @RequestBody PwaOfflineBundleSyncRequest request,
                                                               Authentication authentication) {
        enforce(authentication, "pwa_offline_bundle_sync");
        return ResponseEntity.ok(facadeService.sincronizar(bundleToken, request));
    }

    @GetMapping("/bundles/{bundleToken}/governance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SurfaceSnapshotResponse> governanca(@PathVariable String bundleToken,
                                                              Authentication authentication) {
        enforce(authentication, "pwa_offline_bundle_governance");
        return ResponseEntity.ok(facadeService.governanca(bundleToken));
    }

    @GetMapping("/bundles/{bundleToken}/conflicts/timeline")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SurfaceSnapshotResponse> timelineConflito(@PathVariable String bundleToken,
                                                                    Authentication authentication) {
        enforce(authentication, "pwa_offline_bundle_conflict_timeline");
        return ResponseEntity.ok(facadeService.timelineConflito(bundleToken));
    }

    @GetMapping("/bundles/{bundleToken}/metrics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SurfaceSnapshotResponse> metricas(@PathVariable String bundleToken,
                                                            Authentication authentication) {
        enforce(authentication, "pwa_offline_bundle_metrics");
        return ResponseEntity.ok(facadeService.metricas(bundleToken));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
