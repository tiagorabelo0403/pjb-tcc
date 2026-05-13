package com.tcc.pjb.backend.controller.admin;

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
@RequestMapping("/api/admin/judicial/data-plane")
public class AdminJudicialConnectorDataPlaneController {

    private final AdminJudicialConnectorRuntimeSurfaceFacadeService facadeService;

    public AdminJudicialConnectorDataPlaneController(AdminJudicialConnectorRuntimeSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceSnapshotResponse> national(@RequestParam(defaultValue = "24") long hours) {
        return ResponseEntity.ok(facadeService.dataPlaneNational(hours));
    }

    @GetMapping("/tribunal/{tribunalCodigo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceSnapshotResponse> tribunal(@PathVariable String tribunalCodigo,
                                                            @RequestParam(defaultValue = "24") long hours) {
        return ResponseEntity.ok(facadeService.dataPlaneTribunal(tribunalCodigo, hours));
    }
}
