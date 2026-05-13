package com.tcc.pjb.backend.configs.security.hardening;

import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.observability.systemhealth.PjbFunctionalAvailabilityService;
import com.tcc.pjb.backend.core.observability.systemhealth.PjbFunctionalAvailabilityService.FunctionalReadiness;
import com.tcc.pjb.backend.core.observability.systemhealth.PjbFunctionalDomain;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class PjbFunctionalAvailabilityFilter extends OncePerRequestFilter {

    private final PjbFunctionalAvailabilityProperties properties;
    private final PjbFunctionalAvailabilityService service;

    public PjbFunctionalAvailabilityFilter(PjbFunctionalAvailabilityProperties properties,
                                           PjbFunctionalAvailabilityService service) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.service = Objects.requireNonNull(service, "service");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isEnabled() || isExempt(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }
        PjbFunctionalDomain domain = PjbFunctionalDomain.fromUri(request.getRequestURI());
        FunctionalReadiness readiness = service.readiness(domain);
        if (!readiness.available()) {
            writeProblem(response, domain, readiness, request.getRequestURI());
            return;
        }
        if (properties.isEmitDebugHeaders()) {
            response.setHeader("X-PJB-Functional-Domain", domain.externalName());
            response.setHeader("X-PJB-Functional-Availability", Boolean.toString(readiness.available()));
        }
        filterChain.doFilter(request, response);
    }

    private boolean isExempt(String uri) {
        return uri != null && (uri.startsWith("/actuator/health") || uri.startsWith("/livez") || uri.startsWith("/readyz") || uri.startsWith("/startupz"));
    }

    private void writeProblem(HttpServletResponse response,
                              PjbFunctionalDomain domain,
                              FunctionalReadiness readiness,
                              String instance) throws IOException {
        response.setStatus(properties.getRejectionStatus());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store, max-age=0");
        response.setHeader("Retry-After", "1");
        response.setHeader("X-PJB-Functional-Domain", domain.externalName());
        response.setHeader("X-PJB-Functional-Availability", "false");
        String requestId = RequestContext.getRequestId().orElse("");
        String body = "{" +
                "\"type\":\"https://pjb.local/problems/functional-availability\"," +
                "\"title\":\"Functional Availability Shield\"," +
                "\"status\":" + properties.getRejectionStatus() + "," +
                "\"detail\":\"" + escapeJson(readiness.reason()) + "\"," +
                "\"instance\":\"" + escapeJson(instance) + "\"," +
                "\"requestId\":\"" + escapeJson(requestId) + "\"," +
                "\"domain\":\"" + domain.externalName() + "\"" +
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
