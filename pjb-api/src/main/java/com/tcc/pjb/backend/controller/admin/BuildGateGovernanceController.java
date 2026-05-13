package com.tcc.pjb.backend.controller.admin;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.model.dto.governance.BuildGateEvaluationResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import com.tcc.pjb.backend.service.governance.BuildGateGovernanceService;

@RestController
@RequestMapping("/api/v1/admin/quality-gates/build")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
public class BuildGateGovernanceController {

    private final BuildGateGovernanceService service;
    private final ApiResponseFactory responseFactory;

    public BuildGateGovernanceController(BuildGateGovernanceService service,
                                         ApiResponseFactory responseFactory) {
        this.service = service;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/report")
    public ResponseEntity<ApiQueryResponse<BuildGateEvaluationResponse>> report() {
        return ResponseEntity.ok(responseFactory.queryOk(service.evaluate(), List.of()));
    }
}
