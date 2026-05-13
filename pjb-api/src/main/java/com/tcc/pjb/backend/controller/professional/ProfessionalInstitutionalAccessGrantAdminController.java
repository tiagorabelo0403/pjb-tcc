package com.tcc.pjb.backend.controller.professional;

import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantAdminWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantBatchDecisionRequest;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantBatchIssueRequest;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantBatchOperationResponse;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantDecisionRequest;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantDetailResponse;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantGovernanceWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantIssueRequest;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantOperationalQueueResponse;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantTemplateBatchIssueRequest;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantTemplateCatalogResponse;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalGrantProcessTimelineResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.professional.ProfessionalInstitutionalAccessGrantAdminService;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/professional/access-grants")
@PreAuthorize("hasAnyRole('ADVOGADO','DEFENSOR_PUBLICO','DEFENSOR_PUBLICO_FEDERAL','PROCURADOR','PROCURADORIA_MUNICIPAL','PROCURADORIA_ESTADUAL','PROCURADORIA_FEDERAL','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_TRABALHISTA','JUIZ_ELEITORAL','JUIZ_MILITAR','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO','MAGISTRADO','SERVIDOR','SERVIDOR_FORUM','ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO','ADMINISTRADOR')")
public class ProfessionalInstitutionalAccessGrantAdminController {

    private final ProfessionalInstitutionalAccessGrantAdminService service;
    private final CapabilityRateLimiter rateLimiter;

    public ProfessionalInstitutionalAccessGrantAdminController(ProfessionalInstitutionalAccessGrantAdminService service,
                                                               CapabilityRateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/workspace")
    public ResponseEntity<ProfessionalGrantAdminWorkspaceResponse> workspace() {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, SecurityContextHolder.getContext().getAuthentication(), "PROFESSIONAL_GRANT_WORKSPACE", ApiVersion.latest());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(8)).cachePrivate().mustRevalidate())
                .body(service.workspace());
    }

    @GetMapping("/governance-dashboard")
    public ResponseEntity<ProfessionalGrantGovernanceWorkspaceResponse> governanceDashboard(@RequestParam(value = "status", required = false) String status,
                                                                                             @RequestParam(value = "actorClass", required = false) String actorClass,
                                                                                             @RequestParam(value = "grantType", required = false) String grantType,
                                                                                             @RequestParam(value = "uf", required = false) String uf,
                                                                                             @RequestParam(value = "comarca", required = false) String comarca,
                                                                                             @RequestParam(value = "tribunal", required = false) String tribunal,
                                                                                             @RequestParam(value = "unidadeJudiciariaCodigo", required = false) String unidadeJudiciariaCodigo,
                                                                                             @RequestParam(value = "orgaoColegiadoCodigo", required = false) String orgaoColegiadoCodigo,
                                                                                             @RequestParam(value = "enteCode", required = false) String enteCode,
                                                                                             @RequestParam(value = "limit", defaultValue = "40") int limit) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, SecurityContextHolder.getContext().getAuthentication(), "PROFESSIONAL_GRANT_GOVERNANCE_DASHBOARD", ApiVersion.latest());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(6)).cachePrivate().mustRevalidate())
                .body(service.governanceDashboard(status, actorClass, grantType, uf, comarca, tribunal, unidadeJudiciariaCodigo, orgaoColegiadoCodigo, enteCode, limit));
    }

    @GetMapping("/operational-dashboard")
    public ResponseEntity<ProfessionalGrantOperationalQueueResponse> operationalDashboard(@RequestParam(value = "gabineteCodigo", required = false) String gabineteCodigo,
                                                                                           @RequestParam(value = "unidadeJudiciariaCodigo", required = false) String unidadeJudiciariaCodigo,
                                                                                           @RequestParam(value = "orgaoColegiadoCodigo", required = false) String orgaoColegiadoCodigo,
                                                                                           @RequestParam(value = "enteCode", required = false) String enteCode,
                                                                                           @RequestParam(value = "criticalOnly", defaultValue = "false") boolean criticalOnly,
                                                                                           @RequestParam(value = "limit", defaultValue = "30") int limit) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, SecurityContextHolder.getContext().getAuthentication(), "PROFESSIONAL_GRANT_OPERATIONAL_DASHBOARD", ApiVersion.latest());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(5)).cachePrivate().mustRevalidate())
                .body(service.operationalDashboard(gabineteCodigo, unidadeJudiciariaCodigo, orgaoColegiadoCodigo, enteCode, criticalOnly, limit));
    }

    @GetMapping("/templates")
    public ResponseEntity<ProfessionalGrantTemplateCatalogResponse> templates() {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, SecurityContextHolder.getContext().getAuthentication(), "PROFESSIONAL_GRANT_TEMPLATE_CATALOG", ApiVersion.latest());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(12)).cachePrivate().mustRevalidate())
                .body(service.templates());
    }

    @PostMapping("/template-batch-requests")
    public ResponseEntity<ProfessionalGrantBatchOperationResponse> issueBatchFromTemplate(@RequestBody ProfessionalGrantTemplateBatchIssueRequest request) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, SecurityContextHolder.getContext().getAuthentication(), "PROFESSIONAL_GRANT_TEMPLATE_BATCH_REQUEST", ApiVersion.latest());
        return ResponseEntity.ok(service.issueBatchFromTemplate(request));
    }

    @PostMapping("/requests")
    public ResponseEntity<ProfessionalGrantDetailResponse> issue(@RequestBody ProfessionalGrantIssueRequest request) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, SecurityContextHolder.getContext().getAuthentication(), "PROFESSIONAL_GRANT_REQUEST", ApiVersion.latest());
        return ResponseEntity.ok(service.issue(request));
    }

    @PostMapping("/batch-requests")
    public ResponseEntity<ProfessionalGrantBatchOperationResponse> issueBatch(@RequestBody ProfessionalGrantBatchIssueRequest request) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, SecurityContextHolder.getContext().getAuthentication(), "PROFESSIONAL_GRANT_BATCH_REQUEST", ApiVersion.latest());
        return ResponseEntity.ok(service.issueBatch(request));
    }

    @PostMapping("/batch-approve")
    public ResponseEntity<ProfessionalGrantBatchOperationResponse> approveBatch(@RequestBody ProfessionalGrantBatchDecisionRequest request) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, SecurityContextHolder.getContext().getAuthentication(), "PROFESSIONAL_GRANT_BATCH_APPROVE", ApiVersion.latest());
        return ResponseEntity.ok(service.approveBatch(request));
    }

    @PostMapping("/batch-revoke")
    public ResponseEntity<ProfessionalGrantBatchOperationResponse> revokeBatch(@RequestBody ProfessionalGrantBatchDecisionRequest request) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, SecurityContextHolder.getContext().getAuthentication(), "PROFESSIONAL_GRANT_BATCH_REVOKE", ApiVersion.latest());
        return ResponseEntity.ok(service.revokeBatch(request));
    }

    @PostMapping("/{grantId}/approve")
    public ResponseEntity<ProfessionalGrantDetailResponse> approve(@PathVariable Long grantId,
                                                                   @RequestBody(required = false) ProfessionalGrantDecisionRequest request) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, SecurityContextHolder.getContext().getAuthentication(), "PROFESSIONAL_GRANT_APPROVE", ApiVersion.latest());
        return ResponseEntity.ok(service.approve(grantId, request));
    }

    @PostMapping("/{grantId}/reject")
    public ResponseEntity<ProfessionalGrantDetailResponse> reject(@PathVariable Long grantId,
                                                                  @RequestBody(required = false) ProfessionalGrantDecisionRequest request) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, SecurityContextHolder.getContext().getAuthentication(), "PROFESSIONAL_GRANT_REJECT", ApiVersion.latest());
        return ResponseEntity.ok(service.reject(grantId, request));
    }

    @PostMapping("/{grantId}/revoke")
    public ResponseEntity<ProfessionalGrantDetailResponse> revoke(@PathVariable Long grantId,
                                                                  @RequestBody(required = false) ProfessionalGrantDecisionRequest request) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, SecurityContextHolder.getContext().getAuthentication(), "PROFESSIONAL_GRANT_REVOKE", ApiVersion.latest());
        return ResponseEntity.ok(service.revoke(grantId, request));
    }

    @GetMapping("/{grantId}")
    public ResponseEntity<ProfessionalGrantDetailResponse> detail(@PathVariable Long grantId) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, SecurityContextHolder.getContext().getAuthentication(), "PROFESSIONAL_GRANT_DETAIL", ApiVersion.latest());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(5)).cachePrivate().mustRevalidate())
                .body(service.detail(grantId));
    }

    @GetMapping("/processos/{numero}/timeline")
    public ResponseEntity<ProfessionalGrantProcessTimelineResponse> processTimeline(@PathVariable String numero) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, SecurityContextHolder.getContext().getAuthentication(), "PROFESSIONAL_GRANT_PROCESS_TIMELINE", ApiVersion.latest());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(5)).cachePrivate().mustRevalidate())
                .body(service.processTimeline(numero));
    }
}
