package com.tcc.pjb.backend.controller.processual.observability.business;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.business.ProcessBusinessObservabilityResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import com.tcc.pjb.backend.service.processual.observability.business.ProcessBusinessObservabilityService;

@RestController
@RequestMapping("/api/v1/processual/observability")
public class ProcessBusinessObservabilityController {

    private final ProcessBusinessObservabilityService service;
    private final ApiResponseFactory apiResponseFactory;

    public ProcessBusinessObservabilityController(ProcessBusinessObservabilityService service,
                                                  ApiResponseFactory apiResponseFactory) {
        this.service = service;
        this.apiResponseFactory = apiResponseFactory;
    }

    @GetMapping("/snapshot")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN','SERVIDOR','SERVIDOR_FORUM','JUIZ','MAGISTRADO')")
    public ResponseEntity<ApiQueryResponse<ProcessBusinessObservabilityResponse>> snapshot() {
        ProcessBusinessObservabilityResponse response = service.snapshot();
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.alertas()));
    }
}
