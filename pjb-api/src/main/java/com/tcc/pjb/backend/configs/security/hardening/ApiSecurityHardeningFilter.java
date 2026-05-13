package com.tcc.pjb.backend.configs.security.hardening;

import com.tcc.pjb.backend.core.observability.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiSecurityHardeningFilter extends OncePerRequestFilter {

    private static final List<String> METHOD_OVERRIDE_HEADERS = List.of(
            "X-HTTP-Method-Override",
            "X-Method-Override",
            "X-HTTP-Method"
    );

    private final SecurityHardeningProperties properties;

    public ApiSecurityHardeningFilter(SecurityHardeningProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (shouldRejectByMethod(request)) {
            writeProblem(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method_not_allowed",
                    "Metodo HTTP bloqueado pela camada de seguranca endurecida.", request.getRequestURI());
            return;
        }
        if (shouldRejectByOverrideHeaders(request)) {
            writeProblem(response, HttpServletResponse.SC_BAD_REQUEST, "method_override_not_allowed",
                    "Cabecalhos de override de metodo nao sao aceitos.", request.getRequestURI());
            return;
        }
        filterChain.doFilter(request, response);
        applyResponseHardening(request, response);
    }

    private boolean shouldRejectByMethod(HttpServletRequest request) {
        String method = request.getMethod();
        if (method == null) {
            return false;
        }
        String normalized = method.trim().toUpperCase(Locale.ROOT);
        if (properties.isRejectTrace() && "TRACE".equals(normalized)) {
            return true;
        }
        if (properties.isRejectTrack() && "TRACK".equals(normalized)) {
            return true;
        }
        return properties.isRejectConnect() && "CONNECT".equals(normalized);
    }

    private boolean shouldRejectByOverrideHeaders(HttpServletRequest request) {
        if (!properties.isRejectMethodOverrideHeaders()) {
            return false;
        }
        for (String header : METHOD_OVERRIDE_HEADERS) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank()) {
                return true;
            }
        }
        return false;
    }

    private void applyResponseHardening(HttpServletRequest request, HttpServletResponse response) {
        if (properties.isAddBrowserHardeningHeaders()) {
            if (!response.containsHeader("X-Permitted-Cross-Domain-Policies")) {
                response.setHeader("X-Permitted-Cross-Domain-Policies", "none");
            }
            if (!response.containsHeader("X-DNS-Prefetch-Control")) {
                response.setHeader("X-DNS-Prefetch-Control", "off");
            }
            if (!response.containsHeader("Origin-Agent-Cluster")) {
                response.setHeader("Origin-Agent-Cluster", "?1");
            }
            mergeCsvHeader(response, "Vary", "Origin");
            mergeCsvHeader(response, "Vary", "Authorization");
            mergeCsvHeader(response, "Vary", "X-Request-Id");
        }
        if (properties.isNoStoreSensitiveResponses() && isSensitivePath(request.getRequestURI())) {
            response.setHeader("Cache-Control", "no-store, no-cache, max-age=0, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0L);
        }
    }

    private boolean isSensitivePath(String uri) {
        if (uri == null || uri.isBlank()) {
            return false;
        }
        List<String> sensitivePaths = properties.getSensitivePaths();
        if (sensitivePaths == null || sensitivePaths.isEmpty()) {
            return false;
        }
        for (String prefix : sensitivePaths) {
            if (prefix != null && !prefix.isBlank() && uri.startsWith(prefix.trim())) {
                return true;
            }
        }
        return false;
    }

    private void mergeCsvHeader(HttpServletResponse response, String name, String value) {
        String current = response.getHeader(name);
        if (current == null || current.isBlank()) {
            response.setHeader(name, value);
            return;
        }
        for (String token : current.split(",")) {
            if (value.equalsIgnoreCase(token.trim())) {
                return;
            }
        }
        response.setHeader(name, current + ", " + value);
    }

    private void writeProblem(HttpServletResponse response,
                              int status,
                              String type,
                              String detail,
                              String instance) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String requestId = RequestContext.getRequestId().orElse("");
        String body = "{" +
                "\"type\":\"https://pjb.local/problems/" + type + "\"," +
                "\"title\":\"Request Rejected\"," +
                "\"status\":" + status + "," +
                "\"detail\":\"" + escapeJson(detail) + "\"," +
                "\"instance\":\"" + escapeJson(instance) + "\"," +
                "\"requestId\":\"" + escapeJson(requestId) + "\"" +
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
}
