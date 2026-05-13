package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.integration.judicial.JudicialConnectorPolicyCommand;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.admin.surface.AdminJudicialConnectorSurfaceFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/judicial/policies")
public class AdminJudicialConnectorPolicyController {

    private final AdminJudicialConnectorSurfaceFacadeService facadeService;

    public AdminJudicialConnectorPolicyController(AdminJudicialConnectorSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceSnapshotResponse> report() {
        return ResponseEntity.ok(facadeService.policyReport());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurfaceSnapshotResponse> upsert(@RequestBody JudicialConnectorPolicyCommand command) {
        return ResponseEntity.ok(facadeService.policyUpsert(command));
    }
}
