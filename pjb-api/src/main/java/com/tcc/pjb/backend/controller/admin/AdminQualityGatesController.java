package com.tcc.pjb.backend.controller.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.core.quality.gates.application.PjbQualityGateReadinessApplicationService;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/quality-gates")
@PreAuthorize("hasAnyRole('ADMIN','ADMINISTRADOR')")
public class AdminQualityGatesController {

    private final PjbQualityGateReadinessApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminQualityGatesController(PjbQualityGateReadinessApplicationService applicationService,
                                       ApiResponseFactory apiResponseFactory) {
        this.applicationService = applicationService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiQueryResponse<?>> summary() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.summary(), List.of()));
    }

    @GetMapping("/build")
    public ResponseEntity<ApiQueryResponse<?>> build() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.buildGate(), List.of()));
    }

    @GetMapping("/matrix")
    public ResponseEntity<ApiQueryResponse<?>> matrix() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.matrix(), List.of()));
    }

    @GetMapping("/architecture")
    public ResponseEntity<ApiQueryResponse<?>> architecture() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.architecture(), List.of()));
    }

    @GetMapping("/contracts")
    public ResponseEntity<ApiQueryResponse<?>> contracts() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.contracts(), List.of()));
    }

    @GetMapping("/mutation")
    public ResponseEntity<ApiQueryResponse<?>> mutation() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.mutation(), List.of()));
    }

    @GetMapping("/dast")
    public ResponseEntity<ApiQueryResponse<?>> dast() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.dast(), List.of()));
    }

    @GetMapping("/integration")
    public ResponseEntity<ApiQueryResponse<?>> integration() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.integration(), List.of()));
    }

    @GetMapping("/blockers")
    public ResponseEntity<ApiQueryResponse<?>> blockers() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.blockers(), List.of()));
    }
}
