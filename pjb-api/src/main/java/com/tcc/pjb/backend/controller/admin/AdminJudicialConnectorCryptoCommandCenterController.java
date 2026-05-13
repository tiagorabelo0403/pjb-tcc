package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.admin.surface.AdminJudicialConnectorRuntimeSurfaceFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/judicial/crypto-command-center")
public class AdminJudicialConnectorCryptoCommandCenterController {

    private final AdminJudicialConnectorRuntimeSurfaceFacadeService facadeService;

    public AdminJudicialConnectorCryptoCommandCenterController(AdminJudicialConnectorRuntimeSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/national")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceSnapshotResponse> national(@RequestParam(required = false) Long recentFailureWindowSeconds) {
        return ResponseEntity.ok(facadeService.cryptoCommandCenterNational(recentFailureWindowSeconds));
    }

    @GetMapping("/tribunal/{tribunalCodigo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceSnapshotResponse> tribunal(@PathVariable String tribunalCodigo,
                                                            @RequestParam(required = false) Long recentFailureWindowSeconds) {
        return ResponseEntity.ok(facadeService.cryptoCommandCenterTribunal(tribunalCodigo, recentFailureWindowSeconds));
    }

    @GetMapping("/sessions/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceSnapshotResponse> sessionSummary(@RequestParam(required = false) Long recentFailureWindowSeconds) {
        return ResponseEntity.ok(facadeService.cryptoSessionSummary(recentFailureWindowSeconds));
    }

    @GetMapping("/sessions/summary/{tribunalCodigo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceSnapshotResponse> sessionSummaryByTribunal(@PathVariable String tribunalCodigo,
                                                                            @RequestParam(required = false) Long recentFailureWindowSeconds) {
        return ResponseEntity.ok(facadeService.cryptoSessionSummaryByTribunal(tribunalCodigo, recentFailureWindowSeconds));
    }

    @GetMapping("/packs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceCollectionResponse> packs() {
        return ResponseEntity.ok(facadeService.cryptoPacks());
    }

    @GetMapping("/packs/{system}/{tribunalCodigo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceSnapshotResponse> pack(@PathVariable JudicialSystem system,
                                                        @PathVariable String tribunalCodigo) {
        return ResponseEntity.ok(facadeService.cryptoPack(system, tribunalCodigo));
    }

    @GetMapping("/packs/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceSnapshotResponse> packSummary() {
        return ResponseEntity.ok(facadeService.cryptoPackSummary());
    }
}
