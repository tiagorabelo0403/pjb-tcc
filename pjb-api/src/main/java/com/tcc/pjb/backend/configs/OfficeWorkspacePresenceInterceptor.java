package com.tcc.pjb.backend.configs;

import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeWorkspacePresenceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class OfficeWorkspacePresenceInterceptor implements HandlerInterceptor {

    private final ObjectProvider<OfficeWorkspacePresenceService> officeWorkspacePresenceServiceProvider;

    public OfficeWorkspacePresenceInterceptor(ObjectProvider<OfficeWorkspacePresenceService> officeWorkspacePresenceServiceProvider) {
        this.officeWorkspacePresenceServiceProvider = officeWorkspacePresenceServiceProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        OfficeWorkspacePresenceService service = officeWorkspacePresenceServiceProvider.getIfAvailable();
        if (service != null) {
            service.touchCurrentWorkspace(request.getRequestURI());
        }
        return true;
    }
}
