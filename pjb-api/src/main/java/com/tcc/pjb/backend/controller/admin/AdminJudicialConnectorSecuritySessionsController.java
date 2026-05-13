package com.tcc.pjb.backend.controller.admin;

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
@RequestMapping("/api/admin/judicial/crypto-sessions")
public class AdminJudicialConnectorSecuritySessionsController {

    private final AdminJudicialConnectorRuntimeSurfaceFacadeService facadeService;

    public AdminJudicialConnectorSecuritySessionsController(AdminJudicialConnectorRuntimeSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceCollectionResponse> recent(@RequestParam(required = false) Long windowSeconds) {
        return ResponseEntity.ok(facadeService.securitySessionsRecent(windowSeconds));
    }

    @GetMapping("/recent/{tribunalCodigo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceCollectionResponse> recentByTribunal(@PathVariable String tribunalCodigo,
                                                                      @RequestParam(required = false) Long windowSeconds) {
        return ResponseEntity.ok(facadeService.securitySessionsRecentByTribunal(tribunalCodigo, windowSeconds));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceSnapshotResponse> summary(@RequestParam(required = false) Long windowSeconds) {
        return ResponseEntity.ok(facadeService.securitySessionsSummary(windowSeconds));
    }

    @GetMapping("/summary/{tribunalCodigo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceSnapshotResponse> summaryByTribunal(@PathVariable String tribunalCodigo,
                                                                     @RequestParam(required = false) Long windowSeconds) {
        return ResponseEntity.ok(facadeService.securitySessionsSummaryByTribunal(tribunalCodigo, windowSeconds));
    }
}
