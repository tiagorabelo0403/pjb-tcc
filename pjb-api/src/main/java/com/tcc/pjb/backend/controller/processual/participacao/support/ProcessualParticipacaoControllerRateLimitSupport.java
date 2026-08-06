package com.tcc.pjb.backend.controller.processual.participacao.support;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomainResolver;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import org.springframework.security.core.Authentication;

public final class ProcessualParticipacaoControllerRateLimitSupport {

    private ProcessualParticipacaoControllerRateLimitSupport() {
    }

    public static void enforce(CapabilityRateLimiter rateLimiter,
                               CapabilityRateLimitDomainResolver domainResolver,
                               Authentication authentication,
                               String capability) {
        rateLimiter.enforce(domainResolver.resolve(authentication), authentication, capability, ApiVersion.V1);
    }
}
