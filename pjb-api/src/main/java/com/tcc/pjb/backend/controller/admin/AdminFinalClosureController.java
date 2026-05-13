package com.tcc.pjb.backend.controller.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.core.quality.finalclosure.application.PjbFinalClosureApplicationService;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/final-closure")
@PreAuthorize("hasAnyRole('ADMIN','ADMINISTRADOR')")
public class AdminFinalClosureController {

    private final PjbFinalClosureApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminFinalClosureController(PjbFinalClosureApplicationService applicationService,
                                       ApiResponseFactory apiResponseFactory) {
        this.applicationService = applicationService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiQueryResponse<?>> summary() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.summary(), List.of()));
    }

    @GetMapping("/blockers")
    public ResponseEntity<ApiQueryResponse<?>> blockers() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.blockers(), List.of()));
    }

    @GetMapping("/readiness")
    public ResponseEntity<ApiQueryResponse<?>> readiness() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.readiness(), List.of()));
    }

    @GetMapping("/sweep")
    public ResponseEntity<ApiQueryResponse<?>> sweep() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.sweep(), List.of()));
    }
}
