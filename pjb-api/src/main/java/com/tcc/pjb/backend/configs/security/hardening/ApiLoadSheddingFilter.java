package com.tcc.pjb.backend.configs.security.hardening;

import com.tcc.pjb.backend.core.jobs.runtime.ResizableSemaphore;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.observability.systemhealth.PjbOperationalCrisisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiLoadSheddingFilter extends OncePerRequestFilter {

    private static final int MAX_LANE_BUCKETS = 64;
    private static final long LANE_BUCKET_IDLE_TTL_NANOS = Duration.ofMinutes(20).toNanos();
    private static final long LANE_BUCKET_CLEANUP_INTERVAL_NANOS = Duration.ofSeconds(30).toNanos();

    private final ApiLoadSheddingProperties properties;
    private final PjbOperationalCrisisService crisisService;
    private final Semaphore globalPermits;
    private final ConcurrentHashMap<String, LanePermitBucket> lanePermits = new ConcurrentHashMap<>();
    private final AtomicLong nextLaneCleanupAtNanos = new AtomicLong(System.nanoTime() + LANE_BUCKET_CLEANUP_INTERVAL_NANOS);

    public ApiLoadSheddingFilter(ApiLoadSheddingProperties properties,
                                 PjbOperationalCrisisService crisisService) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.crisisService = Objects.requireNonNull(crisisService, "crisisService");
        this.globalPermits = new Semaphore(properties.getGlobalMaxInFlight(), true);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (!properties.isEnabled() || isExempt(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        compactLaneBuckets(null);
        LaneMatch lane = resolveLane(uri);
        EffectiveLane effectiveLane = lane == null ? null : effectiveLane(lane.rule(), uri);
        if (effectiveLane != null && effectiveLane.crisisBlocked()) {
            writeProblem(
                    response,
                    effectiveLane.rejectionStatus(),
                    effectiveLane.rejectionCode(),
                    effectiveLane.detail(),
                    uri,
                    effectiveLane.name(),
                    effectiveLane.maxInFlight()
            );
            return;
        }
        if (!tryAcquire(globalPermits, properties.getGlobalAcquireTimeout())) {
            writeProblem(response, 503, "LOAD_SHED_GLOBAL", "A borda da API entrou em proteção de carga e rejeitou a requisição temporariamente.", uri, "GLOBAL", properties.getGlobalMaxInFlight());
            return;
        }

        LanePermitBucket laneBucket = effectiveLane == null ? null : laneBucket(effectiveLane);
        Semaphore laneSemaphore = laneBucket == null ? null : laneBucket.semaphore();
        boolean laneAcquired = false;
        try {
            if (laneSemaphore != null) {
                laneAcquired = tryAcquire(laneSemaphore, effectiveLane.acquireTimeout());
                if (!laneAcquired) {
                    writeProblem(
                            response,
                            effectiveLane.rejectionStatus(),
                            effectiveLane.rejectionCode(),
                            effectiveLane.detail(),
                            uri,
                            effectiveLane.name(),
                            effectiveLane.maxInFlight()
                    );
                    return;
                }
            }
            if (properties.isEmitDebugHeaders() || crisisService.emitDebugHeaders()) {
                response.setHeader("X-PJB-Load-Shed-Global-Limit", String.valueOf(properties.getGlobalMaxInFlight()));
                response.setHeader("X-PJB-Load-Shed-Global-Available", String.valueOf(globalPermits.availablePermits()));
                response.setHeader("X-PJB-Load-Shed-Lane", effectiveLane == null ? "DEFAULT" : effectiveLane.name());
                if (effectiveLane != null && laneSemaphore != null) {
                    response.setHeader("X-PJB-Load-Shed-Lane-Limit", String.valueOf(effectiveLane.maxInFlight()));
                    response.setHeader("X-PJB-Load-Shed-Lane-Available", String.valueOf(Math.max(0, laneSemaphore.availablePermits())));
                }
                if (crisisService.isActive()) {
                    response.setHeader("X-PJB-Crisis-Mode", crisisService.mode().externalName());
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            if (laneSemaphore != null && laneAcquired) {
                laneSemaphore.release();
            }
            globalPermits.release();
        }
    }

    private boolean isExempt(String uri) {
        if (uri == null || uri.isBlank()) {
            return false;
        }
        for (String prefix : properties.getExemptPrefixes()) {
            if (prefix != null && !prefix.isBlank() && uri.startsWith(prefix.trim())) {
                return true;
            }
        }
        return false;
    }

    private LaneMatch resolveLane(String uri) {
        if (uri == null || uri.isBlank()) {
            return null;
        }
        for (ApiLoadSheddingProperties.Rule rule : properties.getRules()) {
            if (rule == null || rule.getPrefixes() == null || rule.getPrefixes().isEmpty()) {
                continue;
            }
            for (String prefix : rule.getPrefixes()) {
                if (prefix != null && !prefix.isBlank() && uri.startsWith(prefix.trim())) {
                    return new LaneMatch(rule);
                }
            }
        }
        return null;
    }

    private EffectiveLane effectiveLane(ApiLoadSheddingProperties.Rule rule, String uri) {
        PjbOperationalCrisisService.CrisisDecision crisisDecision = crisisService.evaluate(
                uri,
                rule.getName(),
                rule.getMaxInFlight(),
                rule.getAcquireTimeout(),
                rule.getRejectionStatus(),
                rule.getRejectionCode()
        );
        String detail = crisisDecision.blocked() && crisisDecision.detail() != null && !crisisDecision.detail().isBlank()
                ? crisisDecision.detail()
                : "A faixa operacional desta API entrou em proteção de carga e orienta novo envio em instantes.";
        return new EffectiveLane(
                rule.getName(),
                crisisDecision.laneLimit(),
                crisisDecision.laneAcquireTimeout(),
                crisisDecision.rejectionStatus(),
                crisisDecision.rejectionCode(),
                detail,
                crisisDecision.mode(),
                crisisDecision.blocked()
        );
    }

    private boolean tryAcquire(Semaphore semaphore, Duration timeout) {
        try {
            long millis = timeout == null ? 0L : Math.max(0L, timeout.toMillis());
            if (millis == 0L) {
                return semaphore.tryAcquire();
            }
            return semaphore.tryAcquire(millis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private LanePermitBucket laneBucket(EffectiveLane lane) {
        String key = lane.semaphoreKey();
        LanePermitBucket bucket = lanePermits.compute(key, (ignored, existing) -> {
            if (existing == null) {
                return new LanePermitBucket(lane.maxInFlight());
            }
            existing.resize(lane.maxInFlight());
            existing.touch();
            return existing;
        });
        compactLaneBuckets(key);
        return bucket;
    }

    private void compactLaneBuckets(String protectedKey) {
        long now = System.nanoTime();
        long scheduled = nextLaneCleanupAtNanos.get();
        if (lanePermits.size() <= MAX_LANE_BUCKETS && now < scheduled) {
            return;
        }
        if (!nextLaneCleanupAtNanos.compareAndSet(scheduled, now + LANE_BUCKET_CLEANUP_INTERVAL_NANOS)) {
            return;
        }
        lanePermits.entrySet().removeIf(entry -> !Objects.equals(entry.getKey(), protectedKey) && entry.getValue().removable(now));
        if (lanePermits.size() <= MAX_LANE_BUCKETS) {
            return;
        }
        lanePermits.entrySet().stream()
                .filter(entry -> !Objects.equals(entry.getKey(), protectedKey))
                .filter(entry -> entry.getValue().idle())
                .sorted(Comparator.comparingLong(entry -> entry.getValue().lastTouchedNanos()))
                .limit(Math.max(0, lanePermits.size() - MAX_LANE_BUCKETS))
                .map(Map.Entry::getKey)
                .toList()
                .forEach(lanePermits::remove);
    }

    private void writeProblem(HttpServletResponse response,
                              int status,
                              String type,
                              String detail,
                              String instance,
                              String lane,
                              int limit) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store, max-age=0");
        response.setHeader("Retry-After", "1");
        response.setHeader("X-PJB-Load-Shed", "true");
        response.setHeader("X-PJB-Load-Shed-Lane", lane);
        response.setHeader("X-PJB-Load-Shed-Limit", String.valueOf(limit));
        String requestId = RequestContext.getRequestId().orElse("");
        String body = "{" +
                "\"type\":\"https://pjb.local/problems/" + escapeJson(type) + "\"," +
                "\"title\":\"Load Shed\"," +
                "\"status\":" + status + "," +
                "\"detail\":\"" + escapeJson(detail) + "\"," +
                "\"instance\":\"" + escapeJson(instance) + "\"," +
                "\"requestId\":\"" + escapeJson(requestId) + "\"," +
                "\"lane\":\"" + escapeJson(lane) + "\"," +
                "\"limit\":" + limit +
                "}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    private record LaneMatch(ApiLoadSheddingProperties.Rule rule) {
    }

    private static final class LanePermitBucket {
        private final ResizableSemaphore semaphore;
        private final AtomicLong lastTouchedNanos;

        private LanePermitBucket(int permits) {
            this.semaphore = new ResizableSemaphore(Math.max(1, permits));
            this.lastTouchedNanos = new AtomicLong(System.nanoTime());
        }

        private Semaphore semaphore() {
            return semaphore;
        }

        private void touch() {
            lastTouchedNanos.set(System.nanoTime());
        }

        private void resize(int targetPermits) {
            int sanitized = Math.max(1, targetPermits);
            int current = semaphore.capacity();
            if (sanitized > current) {
                semaphore.expand(sanitized - current);
            } else if (sanitized < current) {
                semaphore.reduce(current - sanitized);
            }
            touch();
        }

        private boolean idle() {
            return semaphore.isIdle();
        }

        private long lastTouchedNanos() {
            return lastTouchedNanos.get();
        }

        private boolean removable(long now) {
            return idle() && now - lastTouchedNanos() >= LANE_BUCKET_IDLE_TTL_NANOS;
        }
    }

    private record EffectiveLane(String name,
                                 int maxInFlight,
                                 Duration acquireTimeout,
                                 int rejectionStatus,
                                 String rejectionCode,
                                 String detail,
                                 String crisisMode,
                                 boolean crisisBlocked) {

        private String semaphoreKey() {
            return name == null || name.isBlank() ? "DEFAULT" : name;
        }
    }
}
