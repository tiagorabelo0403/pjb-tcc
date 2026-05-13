package com.tcc.pjb.backend.ai.financeira.api.v58;

import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.financeira.pipeline.FinanceiraCognitivePipelineOrchestrator;
import com.tcc.pjb.backend.ai.financeira.router.FinanceiraAiVersionSelector;
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
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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
@RequestMapping(path = "/api/ai/financial/mesh", produces = MediaType.APPLICATION_JSON_VALUE)
public class FinanceiraMeshControllerV58 {

    private static final Duration EXECUTION_TIMEOUT = Duration.ofSeconds(20);

    private final FinanceiraAiVersionSelector selector;
    private final FinanceiraCognitivePipelineOrchestrator pipeline;
    private final CanonicalJsonHasher hasher;
    private final Clock pjbClock;
    private final CapabilityRateLimiter capabilityRateLimiter;
    private final AiMicrometerTelemetry telemetry;
    private final PjbExecutionOrchestrator executionOrchestrator;

    public FinanceiraMeshControllerV58(FinanceiraAiVersionSelector selector,
                                       FinanceiraCognitivePipelineOrchestrator pipeline,
                                       CanonicalJsonHasher hasher,
                                       Clock pjbClock,
                                       CapabilityRateLimiter capabilityRateLimiter,
                                       AiMicrometerTelemetry telemetry,
                                       PjbExecutionOrchestrator executionOrchestrator) {
        this.selector = selector;
        this.pipeline = pipeline;
        this.hasher = hasher;
        this.pjbClock = pjbClock;
        this.capabilityRateLimiter = capabilityRateLimiter;
        this.telemetry = telemetry;
        this.executionOrchestrator = executionOrchestrator;
    }

    @PostMapping(path = "/execute", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@financeiraCapabilityRbac.canExecute(authentication, #request)")
    public CompletableFuture<ResponseEntity<IAResponse>> execute(@Valid @RequestBody IARequest request,
                                                                 Authentication authentication,
                                                                 HttpServletRequest http) {
        Objects.requireNonNull(request, "request");
        Instant startedAt = Instant.now(pjbClock);
        Fingerprint fp = hasher.fingerprint(request);

        ApiVersion version = selector.resolveVersion(request);
        String capability = selector.resolveCapability(request);
        CapabilityRateLimitDecision rl = capabilityRateLimiter.evaluate(CapabilityRateLimitDomain.FINANCEIRA, authentication, capability, version);
        if (!rl.allowed()) {
            telemetry.record(AiTelemetryDomain.FINANCE, capability, version, AiOutcomeTag.ofStatusCode(429), Duration.ZERO);
            throw new CapabilityRateLimitExceededException(CapabilityRateLimitDomain.FINANCEIRA, capability, version, rl);
        }
        HttpHeaders rlHeaders = toHeaders(rl);

        log.info("[AI][FINANCEIRA][MESH] start path={} reqId={} corrId={} sha256={} jsonBytes={} gzipBytes={}",
                safePath(http),
                safeToken(request.getRequestId()),
                safeToken(request.getCorrelationId()),
                safeToken(fp.sha256()),
                fp.jsonBytes(),
                fp.gzipBytes());

        return executionOrchestrator
                .supply(PjbExecutionDescriptor.externalIo("ai.financeira.mesh.execute", EXECUTION_TIMEOUT), () -> pipeline.run(request, capability, version))
                .handle((resp, err) -> finalizeResponse(request, capability, version, startedAt, fp, rlHeaders, resp, err));
    }

    private ResponseEntity<IAResponse> finalizeResponse(IARequest request,
                                                        String capability,
                                                        ApiVersion version,
                                                        Instant startedAt,
                                                        Fingerprint fp,
                                                        HttpHeaders rlHeaders,
                                                        IAResponse resp,
                                                        Throwable err) {
        Instant finishedAt = Instant.now(pjbClock);
        long ms = Math.max(0, finishedAt.toEpochMilli() - startedAt.toEpochMilli());
        if (err != null) {
            telemetry.record(AiTelemetryDomain.FINANCE, capability, version, "500_ERROR", Duration.ofMillis(ms));
            log.warn("[AI][FINANCEIRA][MESH] failed reqId={} sha256={} tookMs={} err={}",
                    safeToken(request.getRequestId()),
                    safeToken(fp.sha256()),
                    ms,
                    err.toString());
            throw propagate(err);
        }
        telemetry.record(AiTelemetryDomain.FINANCE, capability, version, "200_OK", Duration.ofMillis(ms));
        log.info("[AI][FINANCEIRA][MESH] done reqId={} sha256={} status={} tookMs={}",
                safeToken(request.getRequestId()),
                safeToken(fp.sha256()),
                resp != null ? resp.getStatus() : null,
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
        if (s.length() > 64) s = s.substring(0, 64);
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
