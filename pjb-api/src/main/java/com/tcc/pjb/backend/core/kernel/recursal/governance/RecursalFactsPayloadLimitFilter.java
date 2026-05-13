package com.tcc.pjb.backend.core.kernel.recursal.governance;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBean(RecursalFactsIngressProperties.class)
@ConditionalOnProperty(prefix = "pjb.recursal.facts", name = "payload-limit-enabled", havingValue = "true", matchIfMissing = true)
public class RecursalFactsPayloadLimitFilter extends OncePerRequestFilter {

    private final RecursalFactsIngressProperties props;

    public RecursalFactsPayloadLimitFilter(RecursalFactsIngressProperties props) {
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!props.isPayloadLimitEnabled()) return true;
        if (!"POST".equalsIgnoreCase(request.getMethod())) return true;
        String uri = request.getRequestURI();
        if (uri == null) return true;

        return !(uri.contains("/intelligence/recursal") && uri.endsWith("/facts"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        long len = request.getContentLengthLong();
        long max = props.getMaxRequestBytes();

        if (len > 0 && max > 0 && len > max) {
            response.setStatus(413);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"PAYLOAD_TOO_LARGE\",\"maxBytes\":" + max + ",\"contentLength\":" + len + "}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
