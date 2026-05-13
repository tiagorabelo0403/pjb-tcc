package com.tcc.pjb.backend.configs.security.hardening;

import com.tcc.pjb.backend.core.observability.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiSurfaceProtectionFilter extends OncePerRequestFilter {

    private final ApiSurfaceProtectionProperties properties;

    public ApiSurfaceProtectionFilter(ApiSurfaceProtectionProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isEnabled() || isExempt(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }
        Rejection rejection = validate(request);
        if (rejection != null) {
            writeProblem(response, rejection.status(), rejection.type(), rejection.detail(), request.getRequestURI());
            return;
        }
        filterChain.doFilter(request, response);
    }

    private Rejection validate(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri != null && uri.length() > properties.getMaxRequestUriLength()) {
            return new Rejection(HttpServletResponse.SC_REQUEST_URI_TOO_LONG, "request_uri_too_long", "URI da requisicao excede o limite permitido.");
        }
        String queryString = request.getQueryString();
        if (queryString != null && queryString.length() > properties.getMaxQueryStringLength()) {
            return new Rejection(HttpServletResponse.SC_REQUEST_URI_TOO_LONG, "query_too_long", "Query string excede o limite permitido.");
        }
        if (properties.isRejectBackslashPath() && uri != null && uri.indexOf('\\') >= 0) {
            return new Rejection(HttpServletResponse.SC_BAD_REQUEST, "invalid_path", "Caminho da requisicao contem padrao nao aceito.");
        }
        if (properties.isRejectDoubleSlashPath() && hasDoubleSlashPath(uri)) {
            return new Rejection(HttpServletResponse.SC_BAD_REQUEST, "invalid_path", "Caminho da requisicao contem padrao nao aceito.");
        }
        if (properties.isRejectPathTraversalTokens() && containsTraversalToken(uri, queryString)) {
            return new Rejection(HttpServletResponse.SC_BAD_REQUEST, "path_traversal_token", "Requisicao contem padrao de caminho nao aceito.");
        }
        if (countHeaders(request) > properties.getMaxHeaderCount()) {
            return new Rejection(HttpServletResponse.SC_BAD_REQUEST, "too_many_headers", "Quantidade de cabecalhos excede o limite permitido.");
        }
        if (hasOversizedHeaderValue(request)) {
            return new Rejection(HttpServletResponse.SC_BAD_REQUEST, "header_value_too_large", "Cabecalho da requisicao excede o limite permitido.");
        }
        if (request.getParameterMap() != null && request.getParameterMap().size() > properties.getMaxParameterCount()) {
            return new Rejection(HttpServletResponse.SC_BAD_REQUEST, "too_many_parameters", "Quantidade de parametros excede o limite permitido.");
        }
        return null;
    }

    private boolean isExempt(String uri) {
        if (uri == null || uri.isBlank()) {
            return false;
        }
        List<String> exemptPrefixes = properties.getExemptPrefixes();
        if (exemptPrefixes == null || exemptPrefixes.isEmpty()) {
            return false;
        }
        for (String prefix : exemptPrefixes) {
            if (prefix != null && !prefix.isBlank() && uri.startsWith(prefix.trim())) {
                return true;
            }
        }
        return false;
    }

    private int countHeaders(HttpServletRequest request) {
        Enumeration<String> names = request.getHeaderNames();
        if (names == null) {
            return 0;
        }
        return Collections.list(names).size();
    }

    private boolean hasOversizedHeaderValue(HttpServletRequest request) {
        Enumeration<String> names = request.getHeaderNames();
        if (names == null) {
            return false;
        }
        int maxHeaderValueLength = properties.getMaxHeaderValueLength();
        for (String name : Collections.list(names)) {
            Enumeration<String> values = request.getHeaders(name);
            if (values == null) {
                continue;
            }
            for (String value : Collections.list(values)) {
                if (value != null && value.length() > maxHeaderValueLength) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasDoubleSlashPath(String uri) {
        if (uri == null || uri.isBlank()) {
            return false;
        }
        int idx = uri.indexOf("//");
        return idx > 0;
    }

    private boolean containsTraversalToken(String uri, String queryString) {
        String combined = ((uri == null ? "" : uri) + " " + (queryString == null ? "" : queryString)).toLowerCase(Locale.ROOT);
        return combined.contains("../")
                || combined.contains("..\\")
                || combined.contains("%2e%2e")
                || combined.contains("%2f%2e%2e")
                || combined.contains("%5c%2e%2e")
                || combined.contains("%252e%252e");
    }

    private void writeProblem(HttpServletResponse response,
                              int status,
                              String type,
                              String detail,
                              String instance) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store, max-age=0");
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

    private record Rejection(int status, String type, String detail) {
    }
}
