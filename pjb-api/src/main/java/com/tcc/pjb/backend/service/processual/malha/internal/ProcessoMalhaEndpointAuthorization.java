package com.tcc.pjb.backend.service.processual.malha.internal;

import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("processoMalhaEndpointAuthorization")
public class ProcessoMalhaEndpointAuthorization {

    private final ProcessoMalhaAuthorizationService processoMalhaAuthorizationService;

    public ProcessoMalhaEndpointAuthorization(ProcessoMalhaAuthorizationService processoMalhaAuthorizationService) {
        this.processoMalhaAuthorizationService = Objects.requireNonNull(processoMalhaAuthorizationService);
    }

    public boolean canAccess(Authentication authentication, String papel) {
        return authentication != null
                && authentication.isAuthenticated()
                && processoMalhaAuthorizationService.canAccessRequestedRole(papel);
    }
}
