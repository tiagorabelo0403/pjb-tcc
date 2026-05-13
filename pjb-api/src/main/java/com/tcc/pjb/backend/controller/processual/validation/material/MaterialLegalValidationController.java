package com.tcc.pjb.backend.controller.processual.validation.material;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.model.dto.processual.validation.material.MaterialLegalValidationRequest;
import com.tcc.pjb.backend.model.dto.processual.validation.material.MaterialLegalValidationResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import com.tcc.pjb.backend.service.processual.validation.material.MaterialLegalValidationService;

@RestController
@RequestMapping("/api/v1/processual/validation")
public class MaterialLegalValidationController {

    private final MaterialLegalValidationService service;
    private final ApiResponseFactory apiResponseFactory;

    public MaterialLegalValidationController(MaterialLegalValidationService service,
                                             ApiResponseFactory apiResponseFactory) {
        this.service = service;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping("/material")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiQueryResponse<MaterialLegalValidationResponse>> validar(@Valid @RequestBody MaterialLegalValidationRequest request) {
        MaterialLegalValidationResponse response = service.validar(request);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.warnings()));
    }
}
