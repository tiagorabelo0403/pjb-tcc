package com.tcc.pjb.backend.controller.admin;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.model.dto.governance.TestQualityMatrixResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import com.tcc.pjb.backend.service.governance.TestQualityMatrixService;

@RestController
@RequestMapping("/api/v1/admin/quality-gates/test-matrix")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
public class TestQualityMatrixController {

    private final TestQualityMatrixService service;
    private final ApiResponseFactory responseFactory;

    public TestQualityMatrixController(TestQualityMatrixService service,
                                       ApiResponseFactory responseFactory) {
        this.service = service;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/report")
    public ResponseEntity<ApiQueryResponse<TestQualityMatrixResponse>> report() {
        return ResponseEntity.ok(responseFactory.queryOk(service.verify(), List.of()));
    }
}
