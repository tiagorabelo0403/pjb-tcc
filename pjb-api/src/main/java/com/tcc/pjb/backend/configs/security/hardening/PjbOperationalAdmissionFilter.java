package com.tcc.pjb.backend.configs.security.hardening;

import com.tcc.pjb.backend.core.observability.RequestContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class PjbOperationalAdmissionFilter extends OncePerRequestFilter {

    private static final String[] PAGE_SIZE_PARAMS = {"size", "limit", "pageSize", "perPage"};
    private static final String[] ITEM_COUNT_PARAMS = {"rows", "maxItems", "maxRows", "estimatedItems", "exportRows"};

    private final PjbOperationalAdmissionProperties properties;
    private final PjbOperationalAdmissionService service;
    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    public PjbOperationalAdmissionFilter(PjbOperationalAdmissionProperties properties,
                                         PjbOperationalAdmissionService service,
                                         MeterRegistry meterRegistry) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.service = Objects.requireNonNull(service, "service");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        PjbOperationalAdmissionService.OperationShape shape = new PjbOperationalAdmissionService.OperationShape(
                Math.max(0L, request.getContentLengthLong()),
                positiveInt(request, PAGE_SIZE_PARAMS),
                positiveLong(request, ITEM_COUNT_PARAMS),
                acceptsEventStream(request)
        );
        PjbOperationalAdmissionService.Decision decision = service.evaluate(request.getMethod(), request.getRequestURI(), shape);
        if (decision.allowed()) {
            increment("allow", decision.code(), decision.bucket(), decision.priority().name().toLowerCase(Locale.ROOT));
            if (properties.isEmitDebugHeaders() && decision.pressure() != null) {
                response.setHeader("X-PJB-Operational-Admission", "allow");
                response.setHeader("X-PJB-Operational-Admission-Code", decision.code());
                response.setHeader("X-PJB-Operational-Admission-Priority", decision.priority().name());
                response.setHeader("X-PJB-Operational-Pressure-Score", Integer.toString(decision.pressure().pressureScore()));
                response.setHeader("X-PJB-Operational-Headroom-Score", Integer.toString(decision.pressure().headroomScore()));
                if (decision.pressure().gc() != null) {
                    response.setHeader("X-PJB-Operational-GC-Pause-Ratio", Double.toString(decision.pressure().gc().pauseRatio()));
                }
            }
            filterChain.doFilter(request, response);
            return;
        }
        increment(decision.hardRejection() ? "reject_hard" : "reject_soft", decision.code(), decision.bucket(), decision.priority().name().toLowerCase(Locale.ROOT));
        writeProblem(response, decision);
    }

    private int positiveInt(HttpServletRequest request, String[] names) {
        for (String name : names) {
            long value = positiveLong(request.getParameter(name));
            if (value > 0L) {
                return (int) Math.min(Integer.MAX_VALUE, value);
            }
        }
        return 0;
    }

    private long positiveLong(HttpServletRequest request, String[] names) {
        for (String name : names) {
            long value = positiveLong(request.getParameter(name));
            if (value > 0L) {
                return value;
            }
        }
        return 0L;
    }

    private long positiveLong(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0L;
        }
        try {
            long value = Long.parseLong(raw.trim());
            return Math.max(0L, value);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private boolean acceptsEventStream(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.toLowerCase(Locale.ROOT).contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }

    private void increment(String outcome, String code, String bucket, String priority) {
        String key = outcome + '|' + code + '|' + bucket + '|' + priority;
        counters.computeIfAbsent(key, ignored -> Counter.builder("pjb.runtime.admission.decisions")
                .tag("outcome", outcome)
                .tag("code", code)
                .tag("bucket", bucket)
                .tag("priority", priority)
                .register(meterRegistry)).increment();
    }

    private void writeProblem(HttpServletResponse response,
                              PjbOperationalAdmissionService.Decision decision) throws IOException {
        response.setStatus(decision.status());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store, max-age=0");
        if (properties.getRetryAfterSeconds() > 0) {
            response.setHeader("Retry-After", Integer.toString(properties.getRetryAfterSeconds()));
        }
        response.setHeader("X-PJB-Operational-Admission", decision.hardRejection() ? "reject-hard" : "reject-soft");
        response.setHeader("X-PJB-Operational-Admission-Code", decision.code());
        response.setHeader("X-PJB-Operational-Admission-Bucket", decision.bucket());
        response.setHeader("X-PJB-Operational-Admission-Priority", decision.priority().name());
        if (decision.pressure() != null) {
            response.setHeader("X-PJB-Operational-Pressure-Score", Integer.toString(decision.pressure().pressureScore()));
            response.setHeader("X-PJB-Operational-Headroom-Score", Integer.toString(decision.pressure().headroomScore()));
            if (decision.pressure().gc() != null) {
                response.setHeader("X-PJB-Operational-GC-Pause-Ratio", Double.toString(decision.pressure().gc().pauseRatio()));
            }
        }
        if (decision.live() != null) {
            response.setHeader("X-PJB-Operational-Live-Subscribers", Long.toString(decision.live().totalSubscribers()));
        }
        if (decision.kafka() != null) {
            response.setHeader("X-PJB-Operational-Kafka-Buffer", Double.toString(decision.kafka().bufferAvailableRatio()));
        }
        String requestId = RequestContext.getRequestId().orElse("");
        String body = "{" +
                "\"type\":\"https://pjb.local/problems/" + escapeJson(decision.code().toLowerCase(Locale.ROOT)) + "\"," +
                "\"title\":\"Operational Admission Control\"," +
                "\"status\":" + decision.status() + "," +
                "\"detail\":\"" + escapeJson(decision.detail()) + "\"," +
                "\"requestId\":\"" + escapeJson(requestId) + "\"," +
                "\"code\":\"" + escapeJson(decision.code()) + "\"," +
                "\"bucket\":\"" + escapeJson(decision.bucket()) + "\"," +
                "\"writeSensitive\":" + decision.writeSensitive() + "," +
                "\"expensive\":" + decision.expensive() +
                (decision.pressure() == null ? "" : ",\"pressureScore\":" + decision.pressure().pressureScore() + ",\"headroomScore\":" + decision.pressure().headroomScore() + (decision.pressure().gc() == null ? "" : ",\"gcPauseRatio\":" + decision.pressure().gc().pauseRatio())) +
                (decision.live() == null ? "" : ",\"liveSubscribers\":" + decision.live().totalSubscribers()) +
                (decision.kafka() == null ? "" : ",\"kafkaBufferAvailableRatio\":" + decision.kafka().bufferAvailableRatio()) +
                "}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}
