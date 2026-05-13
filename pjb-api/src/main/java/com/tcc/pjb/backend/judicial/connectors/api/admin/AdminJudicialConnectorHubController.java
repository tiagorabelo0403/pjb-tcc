package com.tcc.pjb.backend.judicial.connectors.api.admin;

import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.admin.surface.AdminJudicialConnectorHubSurfaceFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/judicial/connectors/hub")
public class AdminJudicialConnectorHubController {

    private final AdminJudicialConnectorHubSurfaceFacadeService surfaceFacadeService;

    public AdminJudicialConnectorHubController(AdminJudicialConnectorHubSurfaceFacadeService surfaceFacadeService) {
        this.surfaceFacadeService = surfaceFacadeService;
    }

    @GetMapping("/national")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceSnapshotResponse> national(@RequestParam(required = false) Long horizonSeconds) {
        return ResponseEntity.ok(surfaceFacadeService.national(horizonSeconds));
    }

    @GetMapping("/tribunal/{tribunalCodigo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceSnapshotResponse> tribunal(@PathVariable String tribunalCodigo,
                                                            @RequestParam(required = false) Long horizonSeconds) {
        return ResponseEntity.ok(surfaceFacadeService.tribunal(tribunalCodigo, horizonSeconds));
    }

    @GetMapping("/structure")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceSnapshotResponse> structure() {
        return ResponseEntity.ok(surfaceFacadeService.structure());
    }
}
