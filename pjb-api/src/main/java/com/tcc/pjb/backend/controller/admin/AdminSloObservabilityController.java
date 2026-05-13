package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.core.observability.PjbSloApplicationService;
import com.tcc.pjb.backend.core.observability.domain.PjbSloExecutionQuery;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/admin/observability/slo")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
public class AdminSloObservabilityController {

    private final PjbSloApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminSloObservabilityController(PjbSloApplicationService applicationService,
                                           ApiResponseFactory apiResponseFactory) {
        this.applicationService = Objects.requireNonNull(applicationService);
        this.apiResponseFactory = Objects.requireNonNull(apiResponseFactory);
    }

    @GetMapping("/registry")
    public ResponseEntity<ApiQueryResponse<?>> registry() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.registrySnapshot(), List.of()));
    }

    @GetMapping("/registry/health")
    public ResponseEntity<ApiQueryResponse<?>> registryHealth() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.registryHealth(), List.of()));
    }

    @GetMapping("/registry/audit")
    public ResponseEntity<ApiQueryResponse<?>> registryAudit() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.registryAudit(), List.of()));
    }

    @GetMapping("/operations/{operation}")
    public ResponseEntity<ApiQueryResponse<?>> operation(@PathVariable String operation) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.operation(operation), List.of()));
    }

    @GetMapping("/operations/{operation}/health")
    public ResponseEntity<ApiQueryResponse<?>> operationHealth(@PathVariable String operation) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.operationHealth(operation), List.of()));
    }

    @GetMapping("/operations/{operation}/audit")
    public ResponseEntity<ApiQueryResponse<?>> operationAudit(@PathVariable String operation) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.operationAudit(operation), List.of()));
    }

    @GetMapping("/operations/{operation}/budget")
    public ResponseEntity<ApiQueryResponse<?>> budget(@PathVariable String operation) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.budget(operation), List.of()));
    }

    @GetMapping("/operations/{operation}/budget/health")
    public ResponseEntity<ApiQueryResponse<?>> budgetHealth(@PathVariable String operation) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.budgetHealth(operation), List.of()));
    }

    @GetMapping("/operations/{operation}/budget/audit")
    public ResponseEntity<ApiQueryResponse<?>> budgetAudit(@PathVariable String operation) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.budgetAudit(operation), List.of()));
    }

    @GetMapping("/operations/{operation}/latency-window")
    public ResponseEntity<ApiQueryResponse<?>> latencyWindow(@PathVariable String operation) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.latencyWindow(operation), List.of()));
    }

    @GetMapping("/operations/{operation}/latency/audit")
    public ResponseEntity<ApiQueryResponse<?>> latencyAudit(@PathVariable String operation) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.latencyAudit(operation), List.of()));
    }

    @GetMapping("/operations/{operation}/consistency")
    public ResponseEntity<ApiQueryResponse<?>> consistency(@PathVariable String operation) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.consistency(operation), List.of()));
    }

    @GetMapping("/operations/{operation}/timeline")
    public ResponseEntity<ApiQueryResponse<?>> timeline(@PathVariable String operation,
                                                        @RequestParam(value = "event", required = false) String event) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.timeline(operation, event), List.of()));
    }

    @GetMapping("/operations/{operation}/evaluate")
    public ResponseEntity<ApiQueryResponse<?>> evaluate(@PathVariable String operation,
                                                        @RequestParam("measuredSeconds") double measuredSeconds) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.evaluate(new PjbSloExecutionQuery(operation, measuredSeconds)), List.of()));
    }

    @GetMapping("/operations/{operation}/violation-snapshot")
    public ResponseEntity<ApiQueryResponse<?>> violationSnapshot(@PathVariable String operation,
                                                                 @RequestParam("measuredSeconds") double measuredSeconds) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.violationSnapshot(operation, measuredSeconds), List.of()));
    }
}
