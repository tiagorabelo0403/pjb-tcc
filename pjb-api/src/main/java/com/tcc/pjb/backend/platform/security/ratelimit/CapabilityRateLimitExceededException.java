package com.tcc.pjb.backend.platform.security.ratelimit;

import java.io.Serial;
import java.util.Objects;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;

public final class CapabilityRateLimitExceededException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final CapabilityRateLimitDomain domain;
    private final String capability;
    private final ApiVersion version;
    private final CapabilityRateLimitDecision decision;

    public CapabilityRateLimitExceededException(CapabilityRateLimitDomain domain,
                                                String capability,
                                                ApiVersion version,
                                                CapabilityRateLimitDecision decision) {
        super("Rate limit excedido");
        this.domain = Objects.requireNonNull(domain, "domain");
        this.capability = capability;
        this.version = Objects.requireNonNull(version, "version");
        this.decision = Objects.requireNonNull(decision, "decision");
    }

    
    public CapabilityRateLimitExceededException(CapabilityRateLimitDomain domain,
                                                String capability,
                                                CapabilityRateLimitDecision decision) {
        this(domain, capability, ApiVersion.latest(), decision);
    }

    public CapabilityRateLimitDomain getDomain() {
        return domain;
    }

    public String getCapability() {
        return capability;
    }

    public ApiVersion getVersion() {
        return version;
    }

    public CapabilityRateLimitDecision getDecision() {
        return decision;
    }
}
