package com.tcc.pjb.backend.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.governance.StructuralAutoRemediationReportResponse;
import com.tcc.pjb.backend.model.dto.governance.StructuralGovernanceReportResponse;
import com.tcc.pjb.backend.service.governance.StructuralGovernanceScannerService;

@RestController
@RequestMapping("/api/v1/admin/governance/structural-scan")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
public class StructuralGovernanceScannerController {

    private final StructuralGovernanceScannerService service;

    public StructuralGovernanceScannerController(StructuralGovernanceScannerService service) {
        this.service = service;
    }

    @GetMapping("/report")
    public ResponseEntity<StructuralGovernanceReportResponse> report() {
        return ResponseEntity.ok(service.scan());
    }

    @GetMapping("/report/detailed")
    public ResponseEntity<StructuralAutoRemediationReportResponse> detailed() {
        return ResponseEntity.ok(service.scanDetailed());
    }
}
