package com.tcc.pjb.backend.controller.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.core.frontend.readiness.application.PjbBackendReadyForFrontendApplicationService;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/frontend-readiness")
@PreAuthorize("hasAnyRole('ADMIN','ADMINISTRADOR')")
public class AdminFrontendReadinessController {

    private final PjbBackendReadyForFrontendApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminFrontendReadinessController(PjbBackendReadyForFrontendApplicationService applicationService,
                                            ApiResponseFactory apiResponseFactory) {
        this.applicationService = applicationService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiQueryResponse<?>> summary() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.summary(), List.of()));
    }

    @GetMapping("/checklist")
    public ResponseEntity<ApiQueryResponse<?>> checklist() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.checklist(), List.of()));
    }

    @GetMapping("/auth")
    public ResponseEntity<ApiQueryResponse<?>> auth() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.authContract(), List.of()));
    }

    @GetMapping("/errors")
    public ResponseEntity<ApiQueryResponse<?>> errors() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.errorContract(), List.of()));
    }

    @GetMapping("/public-routes")
    public ResponseEntity<ApiQueryResponse<?>> publicRoutes() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.publicRoutes(), List.of()));
    }

    @GetMapping("/blockers")
    public ResponseEntity<ApiQueryResponse<?>> blockers() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.blockers(), List.of()));
    }

    @GetMapping("/bootstrap")
    public ResponseEntity<ApiQueryResponse<?>> bootstrap() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.bootstrap(), List.of()));
    }

    @GetMapping("/public-contract/summary")
    public ResponseEntity<ApiQueryResponse<?>> publicContractSummary() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.publicContractSummary(), List.of()));
    }

    @GetMapping("/public-contract/routes")
    public ResponseEntity<ApiQueryResponse<?>> publicContractRoutes() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.publicContractRoutes(), List.of()));
    }

    @GetMapping("/public-contract/dtos")
    public ResponseEntity<ApiQueryResponse<?>> dtoCatalog() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.dtoCatalog(), List.of()));
    }

    @GetMapping("/public-contract/freeze")
    public ResponseEntity<ApiQueryResponse<?>> publicContractFreeze() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.publicContractFreeze(), List.of()));
    }

    @GetMapping("/public-contract/envelopes")
    public ResponseEntity<ApiQueryResponse<?>> envelopeContract() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.envelopeContract(), List.of()));
    }

    @GetMapping("/public-contract/validation")
    public ResponseEntity<ApiQueryResponse<?>> validationContract() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.validationContract(), List.of()));
    }

    @GetMapping("/public-contract/error-catalog")
    public ResponseEntity<ApiQueryResponse<?>> httpErrorCatalog() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.httpErrorCatalog(), List.of()));
    }

    @GetMapping("/public-contract/http-freeze")
    public ResponseEntity<ApiQueryResponse<?>> httpContractFreeze() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.httpContractFreeze(), List.of()));
    }

    @GetMapping("/integration-pack/summary")
    public ResponseEntity<ApiQueryResponse<?>> integrationPackSummary() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.integrationPackSummary(), List.of()));
    }

    @GetMapping("/integration-pack/artifacts")
    public ResponseEntity<ApiQueryResponse<?>> integrationArtifacts() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.integrationArtifacts(), List.of()));
    }

    @GetMapping("/integration-pack/seeds")
    public ResponseEntity<ApiQueryResponse<?>> seedScenarios() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.seedScenarios(), List.of()));
    }

    @GetMapping("/integration-pack/profile")
    public ResponseEntity<ApiQueryResponse<?>> frontendDevProfile() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.frontendDevProfile(), List.of()));
    }

    @GetMapping("/integration-pack/smoke")
    public ResponseEntity<ApiQueryResponse<?>> smokePack() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.smokePack(), List.of()));
    }
}
