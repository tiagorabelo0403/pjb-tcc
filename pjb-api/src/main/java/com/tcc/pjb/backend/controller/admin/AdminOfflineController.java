package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import com.tcc.pjb.backend.service.offline.OfflineApplicationService;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/admin/offline")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
public class AdminOfflineController {

    private final OfflineApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminOfflineController(OfflineApplicationService applicationService,
                                  ApiResponseFactory apiResponseFactory) {
        this.applicationService = Objects.requireNonNull(applicationService);
        this.apiResponseFactory = Objects.requireNonNull(apiResponseFactory);
    }

    @GetMapping("/bundles/{bundleToken}/metrics")
    public ResponseEntity<ApiQueryResponse<?>> metrics(@PathVariable String bundleToken) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.metrics(bundleToken), List.of()));
    }

    @GetMapping("/bundles/{bundleToken}/governance/status")
    public ResponseEntity<ApiQueryResponse<?>> governanceStatus(@PathVariable String bundleToken) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.governanceStatus(bundleToken), List.of()));
    }

    @GetMapping("/bundles/{bundleToken}/conflict/timeline")
    public ResponseEntity<ApiQueryResponse<?>> conflictTimeline(@PathVariable String bundleToken) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.conflictTimeline(bundleToken), List.of()));
    }

    @GetMapping("/bundles/{bundleToken}/consistency")
    public ResponseEntity<ApiQueryResponse<?>> consistency(@PathVariable String bundleToken) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.consistency(bundleToken), List.of()));
    }

    @GetMapping("/bundles/{bundleToken}/ownership")
    public ResponseEntity<ApiQueryResponse<?>> ownership(@PathVariable String bundleToken) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.ownership(bundleToken), List.of()));
    }

    @GetMapping("/bundles/{bundleToken}/expiry")
    public ResponseEntity<ApiQueryResponse<?>> expiry(@PathVariable String bundleToken) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.expiry(bundleToken), List.of()));
    }

    @GetMapping("/bundles/{bundleToken}/envelope")
    public ResponseEntity<ApiQueryResponse<?>> envelope(@PathVariable String bundleToken) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.envelope(bundleToken), List.of()));
    }

    @GetMapping("/bundles/{bundleToken}/signal")
    public ResponseEntity<ApiQueryResponse<?>> signal(@PathVariable String bundleToken) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.signal(bundleToken), List.of()));
    }

    @GetMapping("/bundles/{bundleToken}/window")
    public ResponseEntity<ApiQueryResponse<?>> window(@PathVariable String bundleToken) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.window(bundleToken), List.of()));
    }

    @GetMapping("/bundles/{bundleToken}/decision")
    public ResponseEntity<ApiQueryResponse<?>> decision(@PathVariable String bundleToken) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.decision(bundleToken), List.of()));
    }

    @GetMapping("/bundles/{bundleToken}/replay/health")
    public ResponseEntity<ApiQueryResponse<?>> replayHealth(@PathVariable String bundleToken) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.replayHealth(bundleToken), List.of()));
    }

    @GetMapping("/bundles/{bundleToken}/sync/window")
    public ResponseEntity<ApiQueryResponse<?>> syncWindow(@PathVariable String bundleToken) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.syncWindow(bundleToken), List.of()));
    }

    @GetMapping("/bundles/{bundleToken}/action/health")
    public ResponseEntity<ApiQueryResponse<?>> actionHealth(@PathVariable String bundleToken) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.actionHealth(bundleToken), List.of()));
    }

    @GetMapping("/bundles/{bundleToken}/action/window")
    public ResponseEntity<ApiQueryResponse<?>> actionWindow(@PathVariable String bundleToken) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.actionWindow(bundleToken), List.of()));
    }

    @GetMapping("/bundles/{bundleToken}/manifest/audit")
    public ResponseEntity<ApiQueryResponse<?>> manifestAudit(@PathVariable String bundleToken) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.manifestAudit(bundleToken), List.of()));
    }

    @GetMapping("/bundles/{bundleToken}/replay/audit")
    public ResponseEntity<ApiQueryResponse<?>> replayAudit(@PathVariable String bundleToken) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.replayAudit(bundleToken), List.of()));
    }

    @GetMapping("/bundles/{bundleToken}/governance/audit")
    public ResponseEntity<ApiQueryResponse<?>> governanceAudit(@PathVariable String bundleToken) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.governanceAudit(bundleToken), List.of()));
    }

    @GetMapping("/bundles/{bundleToken}/timeline/health")
    public ResponseEntity<ApiQueryResponse<?>> timelineHealth(@PathVariable String bundleToken) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.timelineHealth(bundleToken), List.of()));
    }
}
