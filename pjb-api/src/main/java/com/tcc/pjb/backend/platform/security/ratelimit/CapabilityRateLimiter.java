package com.tcc.pjb.backend.platform.security.ratelimit;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import com.tcc.pjb.backend.platform.security.rbac.CapabilityStrings;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CapabilityRateLimiter {

    private final CapabilityRateLimitProperties props;
    private final CapabilityRateLimitStore store;
    private final CapabilityRateLimitPolicyResolver resolver;
    private final Clock clock;

    public CapabilityRateLimiter(CapabilityRateLimitProperties props,
                                 CapabilityRateLimitStore store,
                                 Clock pjbClock) {
        this.props = Objects.requireNonNull(props, "props");
        this.store = Objects.requireNonNull(store, "store");
        this.resolver = new CapabilityRateLimitPolicyResolver(props);
        this.clock = Objects.requireNonNull(pjbClock, "pjbClock");
    }

    public CapabilityRateLimitDecision evaluate(CapabilityRateLimitDomain domain,
                                                Authentication authentication,
                                                String capabilityRaw,
                                                ApiVersion version) {
        Objects.requireNonNull(domain, "domain");

        if (!props.isEnabled()) {
            return new CapabilityRateLimitDecision(true, 0, 0, 0, resolver.resolveWindowSeconds(), 0);
        }

        String capability = CapabilityStrings.canonical(capabilityRaw);
        if (capability == null) capability = "UNKNOWN";

        ApiVersion v = (version != null) ? version : ApiVersion.latest();

        String subject = subject(authentication);
        String key = RateLimitKeys.buildKey(props.getKeyPrefix(), domain, capability, v.canonical(), subject);

        int window = resolver.resolveWindowSeconds();
        int limit = resolver.resolveLimitTokens(domain, capability, v);
        int cost = resolver.resolveCostTokens(v);

        long now = Instant.now(clock).getEpochSecond();
        CapabilityRateLimitDecision decision = store.tryConsume(key, now, window, limit, cost);

        
        if (!decision.allowed()) {
            log.warn("[CAP-RL] denied domain={} capability={} limit={} window={} cost={} retryAfter={}s",
                    domain.name(),
                    capability,
                    decision.limitTokens(),
                    decision.windowSeconds(),
                    decision.costTokens(),
                    decision.retryAfterSeconds());
        }

        return decision;
    }

    public CapabilityRateLimitDecision enforce(CapabilityRateLimitDomain domain,
                                               Authentication authentication,
                                               String capabilityRaw,
                                               ApiVersion version) {
        CapabilityRateLimitDecision d = evaluate(domain, authentication, capabilityRaw, version);
        if (!d.allowed()) {
            ApiVersion v = (version != null) ? version : ApiVersion.latest();
            throw new CapabilityRateLimitExceededException(domain, CapabilityStrings.canonical(capabilityRaw), v, d);
        }
        return d;
    }

    private static String subject(Authentication authentication) {
        if (authentication == null) return "anonymous";
        if (!authentication.isAuthenticated()) return "anonymous";
        String n = authentication.getName();
        if (n == null || n.isBlank()) return "anonymous";
        return n.trim();
    }
}
