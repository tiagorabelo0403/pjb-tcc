package com.tcc.pjb.backend.configs.security.legacy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.tcc.pjb.backend.core.observability.RequestContext;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 15)
@ConditionalOnProperty(prefix = "pjb.api.legacy-paths", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LegacyPathsBlockFilter extends OncePerRequestFilter {

    private static final String LEGACY_PREFIX = "/com/tcc/pjb/backend/api";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri != null && (uri.equals(LEGACY_PREFIX) || uri.startsWith(LEGACY_PREFIX + "/"))) {
            writeNotFound(response, uri);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeNotFound(HttpServletResponse response, String instance) throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String requestId = RequestContext.getRequestId().orElse("");

        
        String body = "{" +
                "\"type\":\"https://pjb.local/problems/legacy_disabled\"," +
                "\"title\":\"Not Found\"," +
                "\"status\":404," +
                "\"detail\":\"Endpoint legado desabilitado. Use /api/v1/**\"," +
                "\"instance\":\"" + instance + "\"" +
                "}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }
}
