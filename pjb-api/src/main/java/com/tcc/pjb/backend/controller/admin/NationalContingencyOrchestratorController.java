package com.tcc.pjb.backend.controller.admin;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.model.dto.processual.resilience.NationalContingencyAssessmentRequest;
import com.tcc.pjb.backend.model.dto.processual.resilience.NationalContingencyAssessmentResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import com.tcc.pjb.backend.service.processual.resilience.NationalContingencyOrchestratorService;

@RestController
@RequestMapping("/api/v1/admin/processual/contingency")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN','SERVIDOR','SERVIDOR_FORUM')")
public class NationalContingencyOrchestratorController {

    private final NationalContingencyOrchestratorService service;
    private final ApiResponseFactory responseFactory;

    public NationalContingencyOrchestratorController(NationalContingencyOrchestratorService service,
                                                     ApiResponseFactory responseFactory) {
        this.service = service;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/assess")
    public ResponseEntity<ApiQueryResponse<NationalContingencyAssessmentResponse>> assess(@Valid @RequestBody NationalContingencyAssessmentRequest request) {
        return ResponseEntity.ok(responseFactory.queryOk(service.assess(request), List.of()));
    }
}
