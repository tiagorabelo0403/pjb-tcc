package com.tcc.pjb.backend.configs;

import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialApiObservabilityService;
import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialApiRouteContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@ConditionalOnBean(CalculoJudicialApiObservabilityService.class)
public class CalculoJudicialApiMetricsInterceptor implements HandlerInterceptor {

    private static final String ATTR_CONTEXT = CalculoJudicialApiMetricsInterceptor.class.getName() + ".context";

    private final ObjectProvider<CalculoJudicialApiObservabilityService> observabilityService;

    public CalculoJudicialApiMetricsInterceptor(ObjectProvider<CalculoJudicialApiObservabilityService> observabilityService) {
        this.observabilityService = observabilityService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        CalculoJudicialApiObservabilityService service = observabilityService.getIfAvailable();
        if (service == null) {
            return true;
        }
        CalculoJudicialApiRouteContext context = service.fromRequest(request);
        if (context != null) {
            request.setAttribute(ATTR_CONTEXT, context);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CalculoJudicialApiObservabilityService service = observabilityService.getIfAvailable();
        if (service == null) {
            return;
        }
        Object raw = request.getAttribute(ATTR_CONTEXT);
        CalculoJudicialApiRouteContext context = raw instanceof CalculoJudicialApiRouteContext value ? value : service.fromRequest(request);
        if (context != null) {
            service.record(context, request.getMethod(), response.getStatus());
        }
    }
}
