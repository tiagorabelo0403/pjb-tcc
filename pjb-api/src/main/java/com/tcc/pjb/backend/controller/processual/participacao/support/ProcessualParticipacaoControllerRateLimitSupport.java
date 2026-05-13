package com.tcc.pjb.backend.controller.processual.participacao.support;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.Objects;
import org.springframework.security.core.Authentication;

public final class ProcessualParticipacaoControllerRateLimitSupport {

    private ProcessualParticipacaoControllerRateLimitSupport() {
    }

    public static void enforce(CapabilityRateLimiter rateLimiter,
                               Authentication authentication,
                               String capability) {
        rateLimiter.enforce(resolveDomain(authentication), authentication, capability, ApiVersion.V1);
    }

    private static CapabilityRateLimitDomain resolveDomain(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return CapabilityRateLimitDomain.LAWYER;
        }
        boolean institutional = authentication.getAuthorities().stream()
                .map(authority -> authority == null ? null : authority.getAuthority())
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .anyMatch(authority -> authority.contains("DEFENSOR")
                        || authority.contains("PROCURADOR")
                        || authority.contains("PROMOTOR")
                        || authority.contains("MINISTERIO_PUBLICO")
                        || authority.contains("PROCURADORIA")
                        || authority.contains("DEFENSORIA")
                        || authority.contains("PERITO"));
        return institutional ? CapabilityRateLimitDomain.INSTITUCIONAL : CapabilityRateLimitDomain.LAWYER;
    }
}
