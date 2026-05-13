package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.model.dto.api.ApiCommandResponse;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.platform.security.idempotency.PjbIdempotencyApplicationService;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/admin/idempotency")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
public class AdminIdempotencyController {

    private final PjbIdempotencyApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminIdempotencyController(PjbIdempotencyApplicationService applicationService,
                                      ApiResponseFactory apiResponseFactory) {
        this.applicationService = Objects.requireNonNull(applicationService);
        this.apiResponseFactory = Objects.requireNonNull(apiResponseFactory);
    }

    @GetMapping("/keys/{key}")
    public ResponseEntity<ApiQueryResponse<?>> key(@PathVariable String key) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.key(key), List.of()));
    }

    @GetMapping("/keys/{key}/snapshot")
    public ResponseEntity<ApiQueryResponse<?>> snapshot(@PathVariable String key) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.snapshot(key), List.of()));
    }

    @GetMapping("/keys/{key}/replay")
    public ResponseEntity<ApiQueryResponse<?>> replayView(@PathVariable String key) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.replayView(key), List.of()));
    }

    @GetMapping("/keys/{key}/timeline")
    public ResponseEntity<ApiQueryResponse<?>> timeline(@PathVariable String key) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.timeline(key), List.of()));
    }

    @GetMapping("/keys/{key}/window")
    public ResponseEntity<ApiQueryResponse<?>> window(@PathVariable String key) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.window(key), List.of()));
    }

    @GetMapping("/keys/{key}/audit")
    public ResponseEntity<ApiQueryResponse<?>> audit(@PathVariable String key) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.audit(key), List.of()));
    }

    @GetMapping("/keys/{key}/decision")
    public ResponseEntity<ApiQueryResponse<?>> decision(@PathVariable String key) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.decision(key), List.of()));
    }

    @GetMapping("/keys/{key}/replay/status")
    public ResponseEntity<ApiQueryResponse<?>> replay(@PathVariable String key) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.replay(key), List.of()));
    }

    @GetMapping("/keys/{key}/health")
    public ResponseEntity<ApiQueryResponse<?>> keyHealth(@PathVariable String key) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.keyHealth(key), List.of()));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiQueryResponse<?>> health() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.health(), List.of()));
    }

    @GetMapping("/keys/{key}/execution/health")
    public ResponseEntity<ApiQueryResponse<?>> executionHealth(@PathVariable String key,
                                                               @RequestParam(value = "route", required = false) String route) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.executionHealth(key, route), List.of()));
    }

    @GetMapping("/keys/{key}/status/health")
    public ResponseEntity<ApiQueryResponse<?>> statusHealth(@PathVariable String key) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.statusHealth(key), List.of()));
    }

    @GetMapping("/keys/{key}/replay/health")
    public ResponseEntity<ApiQueryResponse<?>> replayHealth(@PathVariable String key) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.replayHealth(key), List.of()));
    }

    @GetMapping("/budget")
    public ResponseEntity<ApiQueryResponse<?>> budget() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.budget(), List.of()));
    }

    @GetMapping("/budget/health")
    public ResponseEntity<ApiQueryResponse<?>> budgetHealth() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.budgetHealth(), List.of()));
    }

    @GetMapping("/keys/{key}/consistency")
    public ResponseEntity<ApiQueryResponse<?>> consistency(@PathVariable String key) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.consistency(key), List.of()));
    }

    @GetMapping("/keys/{key}/envelope")
    public ResponseEntity<ApiQueryResponse<?>> envelope(@PathVariable String key) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.envelope(key), List.of()));
    }

    @GetMapping("/keys/{key}/signal")
    public ResponseEntity<ApiQueryResponse<?>> signal(@PathVariable String key) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.signal(key), List.of()));
    }

    @GetMapping("/keys/{key}/owner")
    public ResponseEntity<ApiQueryResponse<?>> owner(@PathVariable String key) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.owner(key), List.of()));
    }

    @GetMapping("/keys/{key}/window/view")
    public ResponseEntity<ApiQueryResponse<?>> windowView(@PathVariable String key) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.windowView(key), List.of()));
    }

    @GetMapping("/keys/{key}/audit/entry")
    public ResponseEntity<ApiQueryResponse<?>> auditEntry(@PathVariable String key) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.auditEntry(key), List.of()));
    }

    @PostMapping("/keys/{key}/release")
    public ResponseEntity<ApiCommandResponse<?>> release(@PathVariable String key) {
        return ResponseEntity.ok(apiResponseFactory.commandOk("chave de idempotencia liberada", applicationService.release(key), List.of()));
    }
}
