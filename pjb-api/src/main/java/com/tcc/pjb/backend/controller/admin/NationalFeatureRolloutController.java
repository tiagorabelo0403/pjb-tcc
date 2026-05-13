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
import com.tcc.pjb.backend.model.dto.processual.rollout.NationalFeatureRolloutRequest;
import com.tcc.pjb.backend.model.dto.processual.rollout.NationalFeatureRolloutResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import com.tcc.pjb.backend.service.processual.rollout.NationalFeatureRolloutService;

@RestController
@RequestMapping("/api/v1/admin/processual/rollout")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
public class NationalFeatureRolloutController {

    private final NationalFeatureRolloutService service;
    private final ApiResponseFactory responseFactory;

    public NationalFeatureRolloutController(NationalFeatureRolloutService service,
                                            ApiResponseFactory responseFactory) {
        this.service = service;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/resolve")
    public ResponseEntity<ApiQueryResponse<NationalFeatureRolloutResponse>> resolve(@Valid @RequestBody NationalFeatureRolloutRequest request) {
        return ResponseEntity.ok(responseFactory.queryOk(service.resolve(request), List.of()));
    }
}
