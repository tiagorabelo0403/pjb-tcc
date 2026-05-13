package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.model.dto.governance.CodebaseLearningResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import com.tcc.pjb.backend.service.governance.CodebaseLearningGovernanceService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/quality-gates/codebase-learning")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
public class CodebaseLearningGovernanceController {

    private final CodebaseLearningGovernanceService service;
    private final ApiResponseFactory responseFactory;

    public CodebaseLearningGovernanceController(CodebaseLearningGovernanceService service,
                                                ApiResponseFactory responseFactory) {
        this.service = service;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/report")
    public ResponseEntity<ApiQueryResponse<CodebaseLearningResponse>> report(@RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {
        return ResponseEntity.ok(responseFactory.queryOk(service.report(refresh), List.of()));
    }
}
