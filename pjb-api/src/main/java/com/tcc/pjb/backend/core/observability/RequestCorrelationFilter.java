package com.tcc.pjb.backend.core.observability;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final int MAX_REQUEST_ID_LENGTH = 80;
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._:-]{8,80}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestId = normalizeRequestId(request.getHeader(HEADER_REQUEST_ID));

        response.setHeader(HEADER_REQUEST_ID, requestId);

        final ExceptionBox box = new ExceptionBox();

        RequestContext.run(requestId, () -> {
            try {
                filterChain.doFilter(request, response);
            } catch (Exception e) {
                box.ex = e;
            }
        });

        if (box.ex != null) {
            if (box.ex instanceof IOException io) throw io;
            if (box.ex instanceof ServletException se) throw se;
            if (box.ex instanceof RuntimeException re) throw re;
            throw new ServletException(box.ex);
        }
    }


    private String normalizeRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        String normalized = requestId.trim();
        if (normalized.length() > MAX_REQUEST_ID_LENGTH) {
            normalized = normalized.substring(0, MAX_REQUEST_ID_LENGTH);
        }
        if (SAFE_REQUEST_ID.matcher(normalized).matches()) {
            return normalized;
        }
        StringBuilder out = new StringBuilder(Math.min(normalized.length(), MAX_REQUEST_ID_LENGTH));
        for (int i = 0; i < normalized.length() && out.length() < MAX_REQUEST_ID_LENGTH; i++) {
            char c = normalized.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '.' || c == '_' || c == ':' || c == '-') {
                out.append(c);
            }
        }
        if (out.length() >= 8) {
            return out.toString();
        }
        return UUID.randomUUID().toString().toLowerCase(Locale.ROOT);
    }

    static final class ExceptionBox {
        Exception ex;
    }
}
