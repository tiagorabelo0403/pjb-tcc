package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.service.security.governance.ApiSecurityGovernanceInspectorService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/security/apis")
public class AdminApiSecurityGovernanceController {

    private final ApiSecurityGovernanceInspectorService inspectorService;

    public AdminApiSecurityGovernanceController(ApiSecurityGovernanceInspectorService inspectorService) {
        this.inspectorService = inspectorService;
    }

    @GetMapping("/posture")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ADMINISTRADOR')")
    public ApiSecurityGovernanceInspectorService.ApiSecurityGovernanceReport posture() {
        return inspectorService.inspect();
    }
}
