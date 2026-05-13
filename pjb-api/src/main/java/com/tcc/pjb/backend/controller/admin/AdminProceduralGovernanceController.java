package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.model.dto.admin.governance.AdminProceduralSnapshotResponse;
import com.tcc.pjb.backend.service.admin.surface.AdminProceduralGovernanceFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/procedural/governance")
public class AdminProceduralGovernanceController {

    private final AdminProceduralGovernanceFacadeService adminProceduralGovernanceFacadeService;

    public AdminProceduralGovernanceController(AdminProceduralGovernanceFacadeService adminProceduralGovernanceFacadeService) {
        this.adminProceduralGovernanceFacadeService = adminProceduralGovernanceFacadeService;
    }

    @GetMapping("/architecture")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminProceduralSnapshotResponse> architecture() {
        return ResponseEntity.ok(adminProceduralGovernanceFacadeService.architecture());
    }

    @GetMapping("/legacy-boundary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminProceduralSnapshotResponse> legacyBoundary() {
        return ResponseEntity.ok(adminProceduralGovernanceFacadeService.legacyBoundary());
    }

    @GetMapping("/bootstrap")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminProceduralSnapshotResponse> bootstrap() {
        return ResponseEntity.ok(adminProceduralGovernanceFacadeService.bootstrap());
    }
}
