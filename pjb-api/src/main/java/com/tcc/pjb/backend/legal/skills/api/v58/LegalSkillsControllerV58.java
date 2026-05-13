package com.tcc.pjb.backend.legal.skills.api.v58;

import com.tcc.pjb.backend.legal.skills.contract.LegalSkillResponseContract;
import com.tcc.pjb.backend.ai.juridica.mesh.JuridicaUnifiedMeshProfileService;
import com.tcc.pjb.backend.ai.juridica.spine.JuridicaLegalAiSpineService;
import com.tcc.pjb.backend.legal.skills.router.LegalSkillVersionSelector;
import com.tcc.pjb.backend.legal.skills.v3.LegalSkillRequestV3;
import com.tcc.pjb.backend.platform.hash.CanonicalJsonHasher;
import com.tcc.pjb.backend.platform.hash.Fingerprint;
import com.tcc.pjb.backend.platform.observability.ai.AiMicrometerTelemetry;
import com.tcc.pjb.backend.platform.observability.ai.AiOutcomeTag;
import com.tcc.pjb.backend.platform.observability.ai.AiTelemetryDomain;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionDescriptor;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDecision;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitExceededException;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.util.NullSafeMaps;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(path = "/api/ai/legal/skills", produces = MediaType.APPLICATION_JSON_VALUE)
public class LegalSkillsControllerV58 {

    private static final Duration EXECUTION_TIMEOUT = Duration.ofSeconds(20);

    private final LegalSkillVersionSelector selector;
    private final CanonicalJsonHasher hasher;
    private final Clock pjbClock;
    private final CapabilityRateLimiter capabilityRateLimiter;
    private final AiMicrometerTelemetry telemetry;
    private final PjbExecutionOrchestrator executionOrchestrator;
    private final JuridicaUnifiedMeshProfileService juridicaUnifiedMeshProfileService;
    private final JuridicaLegalAiSpineService juridicaLegalAiSpineService;

    public LegalSkillsControllerV58(LegalSkillVersionSelector selector,
                                    CanonicalJsonHasher hasher,
                                    Clock pjbClock,
                                    CapabilityRateLimiter capabilityRateLimiter,
                                    AiMicrometerTelemetry telemetry,
                                    PjbExecutionOrchestrator executionOrchestrator,
                                    JuridicaUnifiedMeshProfileService juridicaUnifiedMeshProfileService,
                                    JuridicaLegalAiSpineService juridicaLegalAiSpineService) {
        this.selector = selector;
        this.hasher = hasher;
        this.pjbClock = pjbClock;
        this.capabilityRateLimiter = capabilityRateLimiter;
        this.telemetry = telemetry;
        this.executionOrchestrator = executionOrchestrator;
        this.juridicaUnifiedMeshProfileService = juridicaUnifiedMeshProfileService;
        this.juridicaLegalAiSpineService = juridicaLegalAiSpineService;
    }

    @PostMapping(path = "/execute", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@legalSkillsCapabilityRbac.canExecute(authentication, #request)")
    public CompletableFuture<ResponseEntity<LegalSkillResponseContract>> execute(@Valid @RequestBody LegalSkillRequestV3 request,
                                                                                 Authentication authentication,
                                                                                 HttpServletRequest http) {
        Objects.requireNonNull(request, "request");
        Instant startedAt = Instant.now(pjbClock);
        Fingerprint fp = hasher.fingerprint(request);

        ApiVersion version = selector.resolveVersion(request);
        String capability = request.getSkill();
        CapabilityRateLimitDecision rl = capabilityRateLimiter.evaluate(CapabilityRateLimitDomain.LEGAL_SKILLS, authentication, capability, version);
        if (!rl.allowed()) {
            telemetry.record(AiTelemetryDomain.LEGAL, capability, version, AiOutcomeTag.ofStatusCode(429), Duration.ZERO);
            throw new CapabilityRateLimitExceededException(CapabilityRateLimitDomain.LEGAL_SKILLS, capability, version, rl);
        }
        HttpHeaders rlHeaders = toHeaders(rl);

        log.info("[SKILL][LEGAL] start path={} reqId={} corrId={} skill={} contract={} sha256={} jsonBytes={} gzipBytes={}",
                safePath(http),
                safeToken(request.getRequestId()),
                safeToken(request.getCorrelationId()),
                safeToken(request.getSkill()),
                safeToken(request.getContractVersion()),
                safeToken(fp.sha256()),
                fp.jsonBytes(),
                fp.gzipBytes());

        var mesh = juridicaUnifiedMeshProfileService.resolveForSkill(request.getSkill(), version, request.getPayload(), Map.of(
                "usuarioId", request.getUsuarioId(),
                "tenantId", request.getTenantId()
        ));
        var spine = juridicaLegalAiSpineService.resolveForSkill(request.getSkill(), version, request.getPayload());

        Map<String, Object> ctx = NullSafeMaps.linked()
                .put("usuarioId", request.getUsuarioId())
                .put("tenantId", request.getTenantId())
                .put("requestSha256", fp.sha256())
                .put("httpPath", http != null ? http.getRequestURI() : null)
                .put("juridicaMeshProfile", mesh.asMap())
                .put("juridicaLegalTools", mesh.tools().stream().map(tool -> tool.id()).toList())
                .put("juridicaSpineProfile", spine.asMap())
                .put("juridicaStructuredOutputs", spine.structuredOutputs().stream().map(output -> output.schemaId()).toList())
                .build();

        HttpHeaders finalHeaders = new HttpHeaders();
        finalHeaders.putAll(rlHeaders);
        finalHeaders.set("X-Legal-Mesh-Profile", mesh.profileCode());
        finalHeaders.set("X-Legal-Tool-Count", String.valueOf(mesh.tools().size()));
        finalHeaders.set("X-Legal-Spine-Profile", spine.profileCode());
        finalHeaders.set("X-Legal-Structured-Outputs", String.valueOf(spine.structuredOutputs().size()));

        return executionOrchestrator
                .supply(PjbExecutionDescriptor.externalIo("ai.legal.skills.execute", EXECUTION_TIMEOUT), () -> selector.execute(request, ctx))
                .handle((resp, err) -> finalizeResponse(request.getRequestId(), request.getSkill(), capability, version, startedAt, fp, finalHeaders, resp, err));
    }

    private ResponseEntity<LegalSkillResponseContract> finalizeResponse(String requestId,
                                                                        String skill,
                                                                        String capability,
                                                                        ApiVersion version,
                                                                        Instant startedAt,
                                                                        Fingerprint fp,
                                                                        HttpHeaders rlHeaders,
                                                                        LegalSkillResponseContract resp,
                                                                        Throwable err) {
        Instant finishedAt = Instant.now(pjbClock);
        long ms = Math.max(0, finishedAt.toEpochMilli() - startedAt.toEpochMilli());
        if (err != null) {
            telemetry.record(AiTelemetryDomain.LEGAL, capability, version, "500_ERROR", Duration.ofMillis(ms));
            log.warn("[SKILL][LEGAL] failed reqId={} sha256={} skill={} tookMs={} err={}",
                    safeToken(requestId),
                    safeToken(fp.sha256()),
                    safeToken(skill),
                    ms,
                    err.toString());
            throw propagate(err);
        }
        telemetry.record(AiTelemetryDomain.LEGAL, capability, version, "200_OK", Duration.ofMillis(ms));
        log.info("[SKILL][LEGAL] done reqId={} sha256={} skill={} tookMs={}",
                safeToken(requestId),
                safeToken(fp.sha256()),
                resp != null ? safeToken(resp.getSkill()) : null,
                ms);
        return ResponseEntity.ok().headers(rlHeaders).body(resp);
    }

    private RuntimeException propagate(Throwable err) {
        Throwable cause = err instanceof java.util.concurrent.CompletionException completion && completion.getCause() != null
                ? completion.getCause()
                : err;
        if (cause instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new RuntimeException(cause);
    }

    private static HttpHeaders toHeaders(CapabilityRateLimitDecision d) {
        HttpHeaders h = new HttpHeaders();
        if (d == null) return h;
        h.set("X-RateLimit-Limit", String.valueOf(d.limitTokens()));
        h.set("X-RateLimit-Remaining", String.valueOf(d.remainingTokens()));
        return h;
    }

    private static String safeToken(String v) {
        if (v == null) return "null";
        String s = v.trim();
        if (s.length() > 96) s = s.substring(0, 96);
        return s.replaceAll("[^a-zA-Z0-9_.:-]", "_");
    }

    private static String safePath(HttpServletRequest r) {
        if (r == null) return "UNKNOWN";
        String p = r.getRequestURI();
        if (p == null) return "UNKNOWN";
        p = p.trim();
        if (p.length() > 180) p = p.substring(0, 180);
        return p.replaceAll("[^a-zA-Z0-9_./-]", "");
    }
}
