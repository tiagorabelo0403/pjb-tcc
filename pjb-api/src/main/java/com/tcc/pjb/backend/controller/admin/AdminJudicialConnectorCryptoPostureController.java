package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.admin.surface.AdminJudicialConnectorRuntimeSurfaceFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/judicial/crypto-posture")
public class AdminJudicialConnectorCryptoPostureController {

    private final AdminJudicialConnectorRuntimeSurfaceFacadeService facadeService;

    public AdminJudicialConnectorCryptoPostureController(AdminJudicialConnectorRuntimeSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceCollectionResponse> inventory() {
        return ResponseEntity.ok(facadeService.cryptoPostureInventory());
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceSnapshotResponse> summary(@RequestParam(required = false) Long recentFailureWindowSeconds) {
        return ResponseEntity.ok(facadeService.cryptoPostureSummary(recentFailureWindowSeconds));
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceCollectionResponse> refreshAll() {
        return ResponseEntity.ok(facadeService.cryptoPostureRefreshAll());
    }

    @PostMapping("/refresh/{system}/{tribunalCodigo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceSnapshotResponse> refresh(@PathVariable JudicialSystem system,
                                                           @PathVariable String tribunalCodigo) {
        return ResponseEntity.ok(facadeService.cryptoPostureRefresh(system, tribunalCodigo));
    }
}
