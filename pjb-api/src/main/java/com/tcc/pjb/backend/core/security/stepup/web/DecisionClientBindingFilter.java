package com.tcc.pjb.backend.core.security.stepup.web;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.tcc.pjb.backend.core.observability.RequestContext;

@Component
public class DecisionClientBindingFilter extends OncePerRequestFilter {

    public static final String HEADER_WINDOW_BINDING = "X-PJB-Window-Binding";
    public static final String HEADER_TAB_BINDING = "X-PJB-Tab-Binding";
    public static final String HEADER_CLIENT_ROUTE = "X-PJB-Client-Route";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        RequestContext.setClientWindowBinding(request.getHeader(HEADER_WINDOW_BINDING));
        RequestContext.setClientTabBinding(request.getHeader(HEADER_TAB_BINDING));
        RequestContext.setClientRouteBinding(request.getHeader(HEADER_CLIENT_ROUTE));
        filterChain.doFilter(request, response);
    }
}
