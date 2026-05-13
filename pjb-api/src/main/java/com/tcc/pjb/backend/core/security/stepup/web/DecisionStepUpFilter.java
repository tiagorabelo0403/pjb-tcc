package com.tcc.pjb.backend.core.security.stepup.web;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.stepup.DecisionStepUpTokenPayload;
import com.tcc.pjb.backend.core.security.stepup.DecisionStepUpTokenService;

@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBean(DecisionStepUpTokenService.class)
public class DecisionStepUpFilter extends OncePerRequestFilter {

    public static final String HEADER_DECISION_STEPUP = "X-PJB-Decision-StepUp";

    private final DecisionStepUpTokenService tokenService;

    public DecisionStepUpFilter(DecisionStepUpTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader(HEADER_DECISION_STEPUP);
        if (token != null && !token.isBlank()) {
            try {
                DecisionStepUpTokenPayload payload = tokenService.verifyAndDecode(token);
                RequestContext.setDecisionStepUpCredential(payload);
            } catch (IllegalArgumentException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"error\":\"PJB_DECISION_STEPUP_INVALID\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
