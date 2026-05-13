package com.tcc.pjb.backend.configs.datasource;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "pjb.datasource.routing", name = "enabled", havingValue = "true")
public class PjbAdaptiveDataPlaneFilter extends OncePerRequestFilter {

    private final PjbAdaptiveDataPlaneService adaptiveDataPlaneService;
    private final PjbAdaptiveDataPlaneContext adaptiveDataPlaneContext;
    private final PjbDataSourceRoutingProperties properties;

    public PjbAdaptiveDataPlaneFilter(PjbAdaptiveDataPlaneService adaptiveDataPlaneService,
                                      PjbAdaptiveDataPlaneContext adaptiveDataPlaneContext,
                                      PjbDataSourceRoutingProperties properties) {
        this.adaptiveDataPlaneService = Objects.requireNonNull(adaptiveDataPlaneService, "adaptiveDataPlaneService");
        this.adaptiveDataPlaneContext = Objects.requireNonNull(adaptiveDataPlaneContext, "adaptiveDataPlaneContext");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            PjbAdaptiveDataPlaneService.AdaptiveDecision decision = adaptiveDataPlaneService.decide(request);
            adaptiveDataPlaneContext.bind(decision);
            emitHeadersIfEnabled(response, decision);
            filterChain.doFilter(request, response);
        } finally {
            adaptiveDataPlaneContext.clear();
        }
    }

    private void emitHeadersIfEnabled(HttpServletResponse response,
                                      PjbAdaptiveDataPlaneService.AdaptiveDecision decision) {
        if (!properties.getAdaptivePlane().isEnabled() || !properties.getAdaptivePlane().isEmitResponseHeaders() || decision == null) {
            return;
        }
        response.setHeader("X-PJB-Data-Plane-Mode", decision.mode().name());
        response.setHeader("X-PJB-Data-Plane-Reason", sanitize(decision.reason()));
        response.setHeader("X-PJB-Data-Plane-Cache-Recommended", Boolean.toString(decision.cacheRecommended()));
        response.setHeader("X-PJB-Data-Plane-Search-Recommended", Boolean.toString(decision.searchRecommended()));
        response.setHeader("X-PJB-Data-Plane-Async-Recommended", Boolean.toString(decision.asyncRecommended()));
        response.setHeader("X-PJB-Data-Plane-Force-Primary", Boolean.toString(decision.forcePrimary()));
        response.setHeader("X-PJB-Data-Plane-Sovereign-Fallback", Boolean.toString(decision.sovereignFallbackActivated()));
        if (decision.preferredReplicaKey() != null) {
            response.setHeader("X-PJB-Data-Plane-Preferred-Replica", sanitize(decision.preferredReplicaKey()));
        }
        if (decision.scaleProfile() != null) {
            response.setHeader("X-PJB-Data-Plane-Scale-Profile", sanitize(decision.scaleProfile()));
        }
        if (decision.scaleInstanceClass() != null) {
            response.setHeader("X-PJB-Data-Plane-Scale-Instance", sanitize(decision.scaleInstanceClass()));
        }
        if (decision.scaleBranchClass() != null) {
            response.setHeader("X-PJB-Data-Plane-Scale-Branch", sanitize(decision.scaleBranchClass()));
        }
        if (decision.sovereignScope() != null) {
            response.setHeader("X-PJB-Data-Plane-Sovereign-Scope", sanitize(decision.sovereignScope()));
        }
        if (decision.replicaLagSeconds() != null) {
            response.setHeader("X-PJB-Data-Plane-Replica-Lag-Sec", format(decision.replicaLagSeconds()));
        }
        response.setHeader("X-PJB-Data-Plane-Read-Pressure", format(decision.readPressureRatio()));
        response.setHeader("X-PJB-Data-Plane-Write-Pressure", format(decision.writePressureRatio()));
        response.setHeader("X-PJB-Data-Plane-Read-Awaiting", Integer.toString(decision.readThreadsAwaiting()));
        response.setHeader("X-PJB-Data-Plane-Write-Awaiting", Integer.toString(decision.writeThreadsAwaiting()));
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }
}
