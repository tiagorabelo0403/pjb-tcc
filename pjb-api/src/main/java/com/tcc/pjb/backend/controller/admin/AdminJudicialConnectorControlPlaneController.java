package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.admin.surface.AdminJudicialConnectorRuntimeSurfaceFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/judicial/control-plane")
public class AdminJudicialConnectorControlPlaneController {

    private final AdminJudicialConnectorRuntimeSurfaceFacadeService facadeService;

    public AdminJudicialConnectorControlPlaneController(AdminJudicialConnectorRuntimeSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceSnapshotResponse> national() {
        return ResponseEntity.ok(facadeService.controlPlaneNational());
    }

    @GetMapping("/tribunal/{tribunalCodigo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceSnapshotResponse> tribunal(@PathVariable String tribunalCodigo) {
        return ResponseEntity.ok(facadeService.controlPlaneTribunal(tribunalCodigo));
    }

    @GetMapping("/runtime")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceSnapshotResponse> runtime() {
        return ResponseEntity.ok(facadeService.runtimeNational());
    }

    @GetMapping("/runtime/tribunal/{tribunalCodigo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceSnapshotResponse> runtimeTribunal(@PathVariable String tribunalCodigo) {
        return ResponseEntity.ok(facadeService.runtimeTribunal(tribunalCodigo));
    }

    @GetMapping("/integrity")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceSnapshotResponse> integrity() {
        return ResponseEntity.ok(facadeService.integrityNational(24));
    }

    @GetMapping("/integrity/tribunal/{tribunalCodigo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceSnapshotResponse> integrityTribunal(@PathVariable String tribunalCodigo) {
        return ResponseEntity.ok(facadeService.integrityTribunal(tribunalCodigo, 24));
    }
}
