package com.tcc.pjb.backend.core.observability;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public final class JustificativaFilter extends OncePerRequestFilter {

    public static final String HEADER_JUSTIFICATIVA = "X-PJB-Justificativa";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String justificativa = request.getHeader(HEADER_JUSTIFICATIVA);
        if (justificativa != null && !justificativa.isBlank()) {
            justificativa = justificativa.trim();
            if (justificativa.length() > 2048) {
                justificativa = justificativa.substring(0, 2048);
            }
            RequestContext.setJustificativa(justificativa);
        }

        filterChain.doFilter(request, response);
    }
}
