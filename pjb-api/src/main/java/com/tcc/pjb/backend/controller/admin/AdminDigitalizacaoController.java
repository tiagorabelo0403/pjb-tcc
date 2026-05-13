package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.core.digitalizacao.DigitalizacaoApplicationService;
import com.tcc.pjb.backend.core.digitalizacao.domain.DigitalizacaoEngineHealthQuery;
import com.tcc.pjb.backend.model.dto.api.ApiCommandResponse;
import com.tcc.pjb.backend.model.dto.api.ApiQueryResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.Instant;
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
@RequestMapping("/api/v1/admin/digitalizacao")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ADMIN')")
public class AdminDigitalizacaoController {

    private final DigitalizacaoApplicationService applicationService;
    private final ApiResponseFactory apiResponseFactory;

    public AdminDigitalizacaoController(DigitalizacaoApplicationService applicationService,
                                        ApiResponseFactory apiResponseFactory) {
        this.applicationService = Objects.requireNonNull(applicationService);
        this.apiResponseFactory = Objects.requireNonNull(apiResponseFactory);
    }

    @GetMapping("/review-queue")
    public ResponseEntity<ApiQueryResponse<?>> reviewQueue(@RequestParam(value = "limit", defaultValue = "20") int limit) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.reviewQueue(limit), List.of()));
    }

    @GetMapping("/review-queue/consistency")
    public ResponseEntity<ApiQueryResponse<?>> queueConsistency(@RequestParam(value = "limit", defaultValue = "20") int limit) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.queueConsistency(limit), List.of()));
    }

    @PostMapping("/governance/reconcile-stale-processing")
    public ResponseEntity<ApiCommandResponse<?>> reconcileStaleProcessing() {
        return ResponseEntity.ok(apiResponseFactory.commandOk("reconciliação de digitalização executada", applicationService.reconcileStaleProcessing(), List.of()));
    }

    @GetMapping("/engine")
    public ResponseEntity<ApiQueryResponse<?>> engineSnapshot() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.engineSnapshot(), List.of()));
    }

    @GetMapping("/engine/health")
    public ResponseEntity<ApiQueryResponse<?>> engineHealth(@RequestParam(value = "criterio", defaultValue = "ready") String criterio) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.engineHealth(new DigitalizacaoEngineHealthQuery("OCR_PIPELINE", criterio, Instant.now())), List.of()));
    }

    @GetMapping("/engine/audit")
    public ResponseEntity<ApiQueryResponse<?>> engineAudit() {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.engineAudit(), List.of()));
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<ApiQueryResponse<?>> job(@PathVariable Long jobId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.job(jobId), List.of()));
    }

    @GetMapping("/jobs/{jobId}/health")
    public ResponseEntity<ApiQueryResponse<?>> jobHealth(@PathVariable Long jobId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.jobHealth(jobId), List.of()));
    }

    @GetMapping("/jobs/{jobId}/timeline")
    public ResponseEntity<ApiQueryResponse<?>> timeline(@PathVariable Long jobId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.timeline(jobId), List.of()));
    }

    @GetMapping("/jobs/{jobId}/confianca")
    public ResponseEntity<ApiQueryResponse<?>> confianca(@PathVariable Long jobId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.confianca(jobId), List.of()));
    }

    @GetMapping("/jobs/{jobId}/confianca/view")
    public ResponseEntity<ApiQueryResponse<?>> confiancaView(@PathVariable Long jobId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.confiancaView(jobId), List.of()));
    }

    @GetMapping("/jobs/{jobId}/idioma")
    public ResponseEntity<ApiQueryResponse<?>> idioma(@PathVariable Long jobId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.idioma(jobId), List.of()));
    }

    @GetMapping("/jobs/{jobId}/idioma/view")
    public ResponseEntity<ApiQueryResponse<?>> idiomaView(@PathVariable Long jobId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.idiomaView(jobId), List.of()));
    }

    @GetMapping("/jobs/{jobId}/ownership")
    public ResponseEntity<ApiQueryResponse<?>> ownership(@PathVariable Long jobId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.ownership(jobId), List.of()));
    }

    @GetMapping("/jobs/{jobId}/review-ownership")
    public ResponseEntity<ApiQueryResponse<?>> reviewOwnership(@PathVariable Long jobId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.reviewOwnership(jobId), List.of()));
    }

    @GetMapping("/jobs/{jobId}/classification-health")
    public ResponseEntity<ApiQueryResponse<?>> classificationHealth(@PathVariable Long jobId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.classificationHealth(jobId), List.of()));
    }

    @GetMapping("/jobs/{jobId}/review-health")
    public ResponseEntity<ApiQueryResponse<?>> reviewHealth(@PathVariable Long jobId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.reviewHealth(jobId), List.of()));
    }

    @GetMapping("/jobs/{jobId}/pending-review")
    public ResponseEntity<ApiQueryResponse<?>> pendingReview(@PathVariable Long jobId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.pendingReview(jobId), List.of()));
    }

    @GetMapping("/jobs/{jobId}/review-window")
    public ResponseEntity<ApiQueryResponse<?>> reviewWindow(@PathVariable Long jobId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.reviewWindow(jobId), List.of()));
    }

    @GetMapping("/jobs/{jobId}/page-window")
    public ResponseEntity<ApiQueryResponse<?>> pageWindow(@PathVariable Long jobId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.pageWindow(jobId), List.of()));
    }

    @GetMapping("/pages/{pageId}")
    public ResponseEntity<ApiQueryResponse<?>> page(@PathVariable Long pageId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.page(pageId), List.of()));
    }

    @GetMapping("/pages/{pageId}/audit")
    public ResponseEntity<ApiQueryResponse<?>> pageAudit(@PathVariable Long pageId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.pageAudit(pageId), List.of()));
    }

    @GetMapping("/pages/{pageId}/consistency")
    public ResponseEntity<ApiQueryResponse<?>> pageConsistency(@PathVariable Long pageId) {
        return ResponseEntity.ok(apiResponseFactory.queryOk(applicationService.pageConsistency(pageId), List.of()));
    }
}
