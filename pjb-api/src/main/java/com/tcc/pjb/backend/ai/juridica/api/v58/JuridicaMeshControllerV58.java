package com.tcc.pjb.backend.ai.juridica.api.v58;

import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.juridica.pipeline.JuridicaCognitivePipelineOrchestrator;
import com.tcc.pjb.backend.ai.juridica.policy.JuridicaAdaptiveMeshGovernanceService;
import com.tcc.pjb.backend.ai.juridica.router.JuridicaAiVersionSelector;
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
@RequestMapping(path = "/api/ai/legal/mesh", produces = MediaType.APPLICATION_JSON_VALUE)
public class JuridicaMeshControllerV58 {

    private static final Duration EXECUTION_TIMEOUT = Duration.ofSeconds(25);

    private final JuridicaAiVersionSelector selector;
    private final JuridicaAdaptiveMeshGovernanceService governanceService;
    private final JuridicaCognitivePipelineOrchestrator pipeline;
    private final CanonicalJsonHasher hasher;
    private final Clock pjbClock;
    private final CapabilityRateLimiter capabilityRateLimiter;
    private final AiMicrometerTelemetry telemetry;
    private final PjbExecutionOrchestrator executionOrchestrator;

    public JuridicaMeshControllerV58(JuridicaAiVersionSelector selector,
                                     JuridicaAdaptiveMeshGovernanceService governanceService,
                                     JuridicaCognitivePipelineOrchestrator pipeline,
                                     CanonicalJsonHasher hasher,
                                     Clock pjbClock,
                                     CapabilityRateLimiter capabilityRateLimiter,
                                     AiMicrometerTelemetry telemetry,
                                     PjbExecutionOrchestrator executionOrchestrator) {
        this.selector = selector;
        this.governanceService = governanceService;
        this.pipeline = pipeline;
        this.hasher = hasher;
        this.pjbClock = pjbClock;
        this.capabilityRateLimiter = capabilityRateLimiter;
        this.telemetry = telemetry;
        this.executionOrchestrator = executionOrchestrator;
    }

    @PostMapping(path = "/execute", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@juridicaCapabilityRbac.canExecute(authentication, #request)")
    public CompletableFuture<ResponseEntity<IAResponse>> execute(@Valid @RequestBody IARequest request,
                                                                 Authentication authentication,
                                                                 HttpServletRequest http) {
        Objects.requireNonNull(request, "request");
        Instant startedAt = Instant.now(pjbClock);
        JuridicaAdaptiveMeshGovernanceService.GovernedRequest governed = governanceService.govern(request);
        IARequest effectiveRequest = governed.request();
        Fingerprint fp = hasher.fingerprint(effectiveRequest);

        ApiVersion requestedVersion = selector.resolveVersion(request);
        ApiVersion version = governed.effectiveVersion();
        String capability = governed.effectiveCapability();
        CapabilityRateLimitDecision rl = capabilityRateLimiter.evaluate(CapabilityRateLimitDomain.JURIDICA, authentication, capability, version);
        if (!rl.allowed()) {
            telemetry.record(AiTelemetryDomain.LEGAL, capability, version, AiOutcomeTag.ofStatusCode(429), Duration.ZERO);
            throw new CapabilityRateLimitExceededException(CapabilityRateLimitDomain.JURIDICA, capability, version, rl);
        }
        HttpHeaders rlHeaders = toHeaders(rl, requestedVersion, version, governed.governance(), governed.toolPolicy());

        log.info("[AI][JURIDICA][MESH] start path={} reqId={} corrId={} sha256={} jsonBytes={} gzipBytes={} requestedVersion={} effectiveVersion={} capability={} governance={}",
                safePath(http),
                safeToken(effectiveRequest.getRequestId()),
                safeToken(effectiveRequest.getCorrelationId()),
                safeToken(fp.sha256()),
                fp.jsonBytes(),
                fp.gzipBytes(),
                requestedVersion.name(),
                version.name(),
                capability,
                governanceMarker(governed.governance()));

        return executionOrchestrator
                .supply(PjbExecutionDescriptor.externalIo("ai.juridica.mesh.execute", EXECUTION_TIMEOUT), () -> pipeline.run(effectiveRequest, capability, version))
                .handle((resp, err) -> finalizeResponse(effectiveRequest, capability, version, startedAt, fp, rlHeaders, resp, err));
    }

    private ResponseEntity<IAResponse> finalizeResponse(IARequest effectiveRequest,
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
            telemetry.record(AiTelemetryDomain.LEGAL, capability, version, "500_ERROR", Duration.ofMillis(ms));
            log.warn("[AI][JURIDICA][MESH] failed reqId={} sha256={} tookMs={} err={}",
                    safeToken(effectiveRequest.getRequestId()),
                    safeToken(fp.sha256()),
                    ms,
                    err.toString());
            throw propagate(err);
        }
        telemetry.record(AiTelemetryDomain.LEGAL, capability, version, "200_OK", Duration.ofMillis(ms));
        log.info("[AI][JURIDICA][MESH] done reqId={} sha256={} status={} tookMs={} effectiveVersion={} capability={}",
                safeToken(effectiveRequest.getRequestId()),
                safeToken(fp.sha256()),
                resp != null ? resp.getStatus() : null,
                ms,
                version.name(),
                capability);
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

    private static HttpHeaders toHeaders(CapabilityRateLimitDecision d,
                                         ApiVersion requestedVersion,
                                         ApiVersion effectiveVersion,
                                         Map<String, Object> governance,
                                         Map<String, Object> toolPolicy) {
        HttpHeaders h = new HttpHeaders();
        if (d != null) {
            h.set("X-RateLimit-Limit", String.valueOf(d.limitTokens()));
            h.set("X-RateLimit-Remaining", String.valueOf(d.remainingTokens()));
        }
        h.set("X-AI-Requested-Version", requestedVersion == null ? ApiVersion.latest().name() : requestedVersion.name());
        h.set("X-AI-Effective-Version", effectiveVersion == null ? ApiVersion.latest().name() : effectiveVersion.name());
        Object rag = governance == null ? null : governance.get("rag");
        if (rag instanceof Map<?, ?> ragMap && ragMap.get("profile") != null) {
            h.set("X-AI-RAG-Profile", String.valueOf(ragMap.get("profile")));
        }
        Object fusion = governance == null ? null : governance.get("mcpRagFusion");
        if (fusion instanceof Map<?, ?> fusionMap) {
            if (fusionMap.get("profile") != null) {
                h.set("X-AI-Fusion-Profile", String.valueOf(fusionMap.get("profile")));
            }
            Object mcp = fusionMap.get("mcp");
            if (mcp instanceof Map<?, ?> mcpMap && mcpMap.get("toolSearchEnabled") != null) {
                h.set("X-AI-Tool-Search", String.valueOf(mcpMap.get("toolSearchEnabled")));
            }
        }
        Object strategy = governance == null ? null : governance.get("strategicExecution");
        if (strategy instanceof Map<?, ?> strategyMap && strategyMap.get("profile") != null) {
            h.set("X-AI-Strategy-Profile", String.valueOf(strategyMap.get("profile")));
        }
        Object mesh = governance == null ? null : governance.get("juridicaMeshProfile");
        if (mesh instanceof Map<?, ?> meshMap) {
            if (meshMap.get("profileCode") != null) {
                h.set("X-AI-Legal-Mesh", String.valueOf(meshMap.get("profileCode")));
            }
            Object tools = meshMap.get("tools");
            if (tools instanceof java.util.Collection<?> collection) {
                h.set("X-AI-Legal-Tools", String.valueOf(collection.size()));
            }
        }
        if (toolPolicy != null && toolPolicy.get("effectiveMode") != null) {
            h.set("X-AI-MCP-Policy", String.valueOf(toolPolicy.get("effectiveMode")));
        }
        return h;
    }

    private static String governanceMarker(Map<String, Object> governance) {
        if (governance == null || governance.isEmpty()) return "none";
        Object effectiveVersion = governance.get("effectiveVersion");
        Object complexity = governance.get("complexityScore");
        Object risk = governance.get("injectionRiskScore");
        return "v=" + safeToken(String.valueOf(effectiveVersion))
                + ",complexity=" + safeToken(String.valueOf(complexity))
                + ",risk=" + safeToken(String.valueOf(risk));
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
