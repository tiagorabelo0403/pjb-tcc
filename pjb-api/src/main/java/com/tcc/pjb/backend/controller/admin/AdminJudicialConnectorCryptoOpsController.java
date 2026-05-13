package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
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
@RequestMapping("/api/admin/judicial/crypto-ops")
public class AdminJudicialConnectorCryptoOpsController {

    private final AdminJudicialConnectorRuntimeSurfaceFacadeService facadeService;

    public AdminJudicialConnectorCryptoOpsController(AdminJudicialConnectorRuntimeSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/inspect/{system}/{tribunalCodigo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceSnapshotResponse> inspect(@PathVariable JudicialSystem system,
                                                           @PathVariable String tribunalCodigo,
                                                           @RequestParam(required = false) String targetUrl,
                                                           @RequestParam(required = false) String requestedBy,
                                                           @RequestParam(required = false) String correlationId) {
        return ResponseEntity.ok(facadeService.cryptoInspect(system, tribunalCodigo, targetUrl, requestedBy, correlationId));
    }

    @GetMapping("/probe/{system}/{tribunalCodigo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceSnapshotResponse> probe(@PathVariable JudicialSystem system,
                                                         @PathVariable String tribunalCodigo,
                                                         @RequestParam(required = false) String targetUrl,
                                                         @RequestParam(required = false) String requestedBy,
                                                         @RequestParam(required = false) String correlationId) {
        return ResponseEntity.ok(facadeService.cryptoProbe(system, tribunalCodigo, targetUrl, requestedBy, correlationId));
    }
}
