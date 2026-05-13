package com.tcc.pjb.backend.controller.processual.lifecycle;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityConsistencyResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityDecisionGateResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityIntegrationResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityObservabilityResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityProductionSealResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityReadinessResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityRemediationResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import com.tcc.pjb.backend.service.casefile.CaseContinuityConsistencyService;
import com.tcc.pjb.backend.service.casefile.CaseContinuityDecisionGateService;
import com.tcc.pjb.backend.service.casefile.CaseContinuityIntegrationService;
import com.tcc.pjb.backend.service.casefile.CaseContinuityObservabilityService;
import com.tcc.pjb.backend.service.casefile.CaseContinuityOrchestratorService;
import com.tcc.pjb.backend.service.casefile.CaseContinuityProductionSealService;
import com.tcc.pjb.backend.service.casefile.CaseContinuityReadinessService;
import com.tcc.pjb.backend.service.casefile.CaseContinuityRemediationService;
import com.tcc.pjb.backend.service.casefile.CaseContinuitySnapshot;

@RestController
@RequestMapping("/api/v1/processual/case-continuity")
public class CaseContinuityController {

    private final CaseContinuityOrchestratorService service;
    private final CaseContinuityObservabilityService observabilityService;
    private final CaseContinuityConsistencyService consistencyService;
    private final CaseContinuityReadinessService readinessService;
    private final CaseContinuityIntegrationService integrationService;
    private final CaseContinuityDecisionGateService decisionGateService;
    private final CaseContinuityRemediationService remediationService;
    private final CaseContinuityProductionSealService productionSealService;
    private final ApiResponseFactory apiResponseFactory;

    public CaseContinuityController(CaseContinuityOrchestratorService service,
                                    CaseContinuityObservabilityService observabilityService,
                                    CaseContinuityConsistencyService consistencyService,
                                    CaseContinuityReadinessService readinessService,
                                    CaseContinuityIntegrationService integrationService,
                                    CaseContinuityDecisionGateService decisionGateService,
                                    CaseContinuityRemediationService remediationService,
                                    CaseContinuityProductionSealService productionSealService,
                                    ApiResponseFactory apiResponseFactory) {
        this.service = service;
        this.observabilityService = observabilityService;
        this.consistencyService = consistencyService;
        this.readinessService = readinessService;
        this.integrationService = integrationService;
        this.decisionGateService = decisionGateService;
        this.remediationService = remediationService;
        this.productionSealService = productionSealService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @GetMapping("/{processoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CaseContinuitySnapshot> inspect(@PathVariable Long processoId) {
        return ResponseEntity.ok(service.inspect(processoId));
    }

    @GetMapping("/{processoId}/observability")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiQueryResponse<CaseContinuityObservabilityResponse>> observability(@PathVariable Long processoId) {
        CaseContinuityObservabilityResponse response = observabilityService.snapshot(processoId);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.warnings()));
    }

    @GetMapping("/{processoId}/consistency")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiQueryResponse<CaseContinuityConsistencyResponse>> consistency(@PathVariable Long processoId) {
        CaseContinuityConsistencyResponse response = consistencyService.snapshot(processoId);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.inconsistencies()));
    }

    @GetMapping("/{processoId}/readiness")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiQueryResponse<CaseContinuityReadinessResponse>> readiness(@PathVariable Long processoId) {
        CaseContinuityReadinessResponse response = readinessService.snapshot(processoId);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.blockers()));
    }

    @GetMapping("/{processoId}/integration")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiQueryResponse<CaseContinuityIntegrationResponse>> integration(@PathVariable Long processoId) {
        CaseContinuityIntegrationResponse response = integrationService.snapshot(processoId);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.blockers()));
    }

    @GetMapping("/{processoId}/gate/{action}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiQueryResponse<CaseContinuityDecisionGateResponse>> gate(@PathVariable Long processoId,
                                                                                      @PathVariable ProcessoLifecycleAction action) {
        CaseContinuityDecisionGateResponse response = decisionGateService.snapshot(processoId, action);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.blockers()));
    }

    @GetMapping("/{processoId}/remediation")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiQueryResponse<CaseContinuityRemediationResponse>> remediation(@PathVariable Long processoId) {
        CaseContinuityRemediationResponse response = remediationService.snapshot(processoId);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.blockers()));
    }

    @GetMapping("/{processoId}/production-seal")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiQueryResponse<CaseContinuityProductionSealResponse>> productionSeal(@PathVariable Long processoId) {
        CaseContinuityProductionSealResponse response = productionSealService.snapshot(processoId);
        return ResponseEntity.ok(apiResponseFactory.queryOk(response, response.blockers()));
    }
}
