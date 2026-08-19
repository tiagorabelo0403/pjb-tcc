package com.tcc.pjb.backend.platform.security.ratelimit;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class CapabilityRateLimitDomainResolver {

    public CapabilityRateLimitDomain resolve(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return CapabilityRateLimitDomain.CITIZEN;
        }
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .map(s -> s.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (authorities.contains("ROLE_ADVOGADO") || authorities.contains("ROLE_ADVOCACIA")) {
            return CapabilityRateLimitDomain.LAWYER;
        }
        if (authorities.contains("ROLE_CIDADAO")) {
            return CapabilityRateLimitDomain.CITIZEN;
        }
        return CapabilityRateLimitDomain.INSTITUCIONAL;
    }
}
