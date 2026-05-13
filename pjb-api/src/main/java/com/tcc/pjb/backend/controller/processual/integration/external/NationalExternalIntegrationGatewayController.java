package com.tcc.pjb.backend.controller.processual.integration.external;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.model.dto.processual.integration.external.ExternalIntegrationDiagnosticRequest;
import com.tcc.pjb.backend.model.dto.processual.integration.external.ExternalIntegrationDiagnosticResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import com.tcc.pjb.backend.service.processual.integration.external.NationalExternalIntegrationGatewayService;

@RestController
@RequestMapping("/api/v1/processual/integration")
public class NationalExternalIntegrationGatewayController {

    private final NationalExternalIntegrationGatewayService service;
    private final ApiResponseFactory apiResponseFactory;

    public NationalExternalIntegrationGatewayController(NationalExternalIntegrationGatewayService service,
                                                        ApiResponseFactory apiResponseFactory) {
        this.service = service;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/diagnostico")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiQueryResponse<ExternalIntegrationDiagnosticResponse>> diagnostico(@Valid @RequestBody ExternalIntegrationDiagnosticRequest request) {
        ExternalIntegrationDiagnosticResponse response = service.diagnosticar(request);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.warnings()));
    }
}
