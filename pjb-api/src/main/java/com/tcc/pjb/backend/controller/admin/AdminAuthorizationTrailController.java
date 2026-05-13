package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationRiskLevel;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationTrailAdminService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationTrailAnalyticsService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationTrailForensicsService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationTrailSourceMode;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationTrailTemporalGranularity;
import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailAnalyticsMaterializationResponse;
import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailAnalyticsOperationalStatusResponse;
import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse;
import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailAnalyticsRefreshQueueStatusResponse;
import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailAnalyticsResponse;
import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailForensicsResponse;
import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailQueryResponse;
import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailRetentionResponse;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/security/authz-trails")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN','MAGISTRADO','JUIZ','DESEMBARGADOR','MINISTRO','SERVIDOR','SERVIDOR_FORUM')")
public class AdminAuthorizationTrailController {

    private final PjbAuthorizationTrailAdminService service;
    private final PjbAuthorizationTrailForensicsService forensicsService;
    private final PjbAuthorizationTrailAnalyticsService analyticsService;

    public AdminAuthorizationTrailController(PjbAuthorizationTrailAdminService service,
                                             PjbAuthorizationTrailForensicsService forensicsService,
                                             PjbAuthorizationTrailAnalyticsService analyticsService) {
        this.service = service;
        this.forensicsService = forensicsService;
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public ResponseEntity<PjbAuthorizationTrailQueryResponse> search(@RequestParam(required = false) PjbAuthorizationTrailSourceMode sourceMode,
                                                                     @RequestParam(required = false) String actionPrefix,
                                                                     @RequestParam(required = false) String resourceType,
                                                                     @RequestParam(required = false) String resourceId,
                                                                     @RequestParam(required = false) Long actorId,
                                                                     @RequestParam(required = false) String actorType,
                                                                     @RequestParam(required = false) Boolean allowed,
                                                                     @RequestParam(required = false) PjbAuthorizationRiskLevel riskLevel,
                                                                     @RequestParam(required = false) String requestId,
                                                                     @RequestParam(required = false) String integrationCode,
                                                                     @RequestParam(required = false) String institutionalUnitCode,
                                                                     @RequestParam(required = false) String institutionalBoxCode,
                                                                     @RequestParam(required = false) String institutionalCapabilityCode,
                                                                     @RequestParam(required = false) String governanceChannel,
                                                                     @RequestParam(required = false) String governanceScope,
                                                                     @RequestParam(required = false) Boolean governanceRequired,
                                                                     @RequestParam(required = false) Boolean governanceSatisfied,
                                                                     @RequestParam(required = false) Boolean stepUpRequired,
                                                                     @RequestParam(required = false) Boolean stepUpSatisfied,
                                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant occurredAfter,
                                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant occurredBefore,
                                                                     @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(service.search(
                sourceMode,
                actionPrefix,
                resourceType,
                resourceId,
                actorId,
                actorType,
                allowed,
                riskLevel,
                requestId,
                integrationCode,
                institutionalUnitCode,
                institutionalBoxCode,
                institutionalCapabilityCode,
                governanceChannel,
                governanceScope,
                governanceRequired,
                governanceSatisfied,
                stepUpRequired,
                stepUpSatisfied,
                occurredAfter,
                occurredBefore,
                limit
        ));
    }

    @GetMapping("/forensics")
    public ResponseEntity<PjbAuthorizationTrailForensicsResponse> forensics(@RequestParam(required = false) PjbAuthorizationTrailTemporalGranularity granularity,
                                                                            @RequestParam(required = false) String actionPrefix,
                                                                            @RequestParam(required = false) String resourceType,
                                                                            @RequestParam(required = false) String resourceId,
                                                                            @RequestParam(required = false) Long actorId,
                                                                            @RequestParam(required = false) String actorType,
                                                                            @RequestParam(required = false) Boolean allowed,
                                                                            @RequestParam(required = false) PjbAuthorizationRiskLevel riskLevel,
                                                                            @RequestParam(required = false) String requestId,
                                                                            @RequestParam(required = false) String integrationCode,
                                                                            @RequestParam(required = false) String institutionalUnitCode,
                                                                            @RequestParam(required = false) String institutionalBoxCode,
                                                                            @RequestParam(required = false) String institutionalCapabilityCode,
                                                                            @RequestParam(required = false) String governanceChannel,
                                                                            @RequestParam(required = false) String governanceScope,
                                                                            @RequestParam(required = false) Boolean governanceRequired,
                                                                            @RequestParam(required = false) Boolean governanceSatisfied,
                                                                            @RequestParam(required = false) Boolean stepUpRequired,
                                                                            @RequestParam(required = false) Boolean stepUpSatisfied,
                                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant occurredAfter,
                                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant occurredBefore,
                                                                            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(forensicsService.forensics(
                granularity,
                actionPrefix,
                resourceType,
                resourceId,
                actorId,
                actorType,
                allowed,
                riskLevel,
                requestId,
                integrationCode,
                institutionalUnitCode,
                institutionalBoxCode,
                institutionalCapabilityCode,
                governanceChannel,
                governanceScope,
                governanceRequired,
                governanceSatisfied,
                stepUpRequired,
                stepUpSatisfied,
                occurredAfter,
                occurredBefore,
                limit
        ));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> export(@RequestParam(required = false) String actionPrefix,
                                         @RequestParam(required = false) String resourceType,
                                         @RequestParam(required = false) String resourceId,
                                         @RequestParam(required = false) Long actorId,
                                         @RequestParam(required = false) String actorType,
                                         @RequestParam(required = false) Boolean allowed,
                                         @RequestParam(required = false) PjbAuthorizationRiskLevel riskLevel,
                                         @RequestParam(required = false) String requestId,
                                         @RequestParam(required = false) String integrationCode,
                                         @RequestParam(required = false) String institutionalUnitCode,
                                         @RequestParam(required = false) String institutionalBoxCode,
                                         @RequestParam(required = false) String institutionalCapabilityCode,
                                         @RequestParam(required = false) String governanceChannel,
                                         @RequestParam(required = false) String governanceScope,
                                         @RequestParam(required = false) Boolean governanceRequired,
                                         @RequestParam(required = false) Boolean governanceSatisfied,
                                         @RequestParam(required = false) Boolean stepUpRequired,
                                         @RequestParam(required = false) Boolean stepUpSatisfied,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant occurredAfter,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant occurredBefore,
                                         @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(forensicsService.exportCsv(
                actionPrefix,
                resourceType,
                resourceId,
                actorId,
                actorType,
                allowed,
                riskLevel,
                requestId,
                integrationCode,
                institutionalUnitCode,
                institutionalBoxCode,
                institutionalCapabilityCode,
                governanceChannel,
                governanceScope,
                governanceRequired,
                governanceSatisfied,
                stepUpRequired,
                stepUpSatisfied,
                occurredAfter,
                occurredBefore,
                limit
        ));
    }

    @GetMapping("/retention")
    public ResponseEntity<PjbAuthorizationTrailRetentionResponse> retention(@RequestParam(required = false) Long retentionDays) {
        return ResponseEntity.ok(forensicsService.retention(retentionDays));
    }

    @GetMapping("/analytics")
    public ResponseEntity<PjbAuthorizationTrailAnalyticsResponse> analytics(@RequestParam(required = false) PjbAuthorizationTrailTemporalGranularity granularity,
                                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant occurredAfter,
                                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant occurredBefore,
                                                                            @RequestParam(required = false) Integer topN,
                                                                            @RequestParam(required = false) Boolean materializeOnRead,
                                                                            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(analyticsService.analytics(
                granularity,
                occurredAfter,
                occurredBefore,
                topN,
                materializeOnRead,
                limit
        ));
    }

    @GetMapping("/analytics/status")
    public ResponseEntity<PjbAuthorizationTrailAnalyticsOperationalStatusResponse> analyticsStatus() {
        return ResponseEntity.ok(analyticsService.status());
    }

    @GetMapping("/analytics/refresh-queue/status")
    public ResponseEntity<PjbAuthorizationTrailAnalyticsRefreshQueueStatusResponse> analyticsRefreshQueueStatus() {
        return ResponseEntity.ok(analyticsService.queueStatus());
    }

    @PostMapping("/analytics/refresh-queue/drain")
    public ResponseEntity<PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse> drainAnalyticsRefreshQueue(@RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(analyticsService.drainQueue(limit));
    }

    @PostMapping("/analytics/refresh-queue/recover-stale")
    public ResponseEntity<PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse> recoverStaleAnalyticsRefreshQueue() {
        return ResponseEntity.ok(analyticsService.recoverStaleQueue());
    }

    @PostMapping("/analytics/refresh-queue/cleanup")
    public ResponseEntity<PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse> cleanupAnalyticsRefreshQueue() {
        return ResponseEntity.ok(analyticsService.cleanupCompletedQueue());
    }

    @PostMapping("/analytics/refresh-queue/backfill")
    public ResponseEntity<PjbAuthorizationTrailAnalyticsRefreshQueueOperationResponse> backfillAnalyticsRefreshQueue(@RequestParam(required = false) PjbAuthorizationTrailTemporalGranularity granularity,
                                                                                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant occurredAfter,
                                                                                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant occurredBefore) {
        return ResponseEntity.ok(analyticsService.backfillQueue(granularity, occurredAfter, occurredBefore));
    }

    @PostMapping("/analytics/materialize")
    public ResponseEntity<PjbAuthorizationTrailAnalyticsMaterializationResponse> materializeAnalytics(@RequestParam(required = false) PjbAuthorizationTrailTemporalGranularity granularity,
                                                                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant occurredAfter,
                                                                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant occurredBefore,
                                                                                                      @RequestParam(required = false) Integer limit,
                                                                                                      @RequestParam(required = false) Boolean replaceExisting) {
        return ResponseEntity.ok(analyticsService.materialize(
                granularity,
                occurredAfter,
                occurredBefore,
                limit,
                replaceExisting
        ));
    }

    @PostMapping("/analytics/refresh-bucket")
    public ResponseEntity<PjbAuthorizationTrailAnalyticsMaterializationResponse> refreshAnalyticsBucket(@RequestParam(required = false) PjbAuthorizationTrailTemporalGranularity granularity,
                                                                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant bucketAt) {
        return ResponseEntity.ok(analyticsService.refreshBucket(granularity, bucketAt));
    }
}
