package com.tcc.pjb.backend.core.security.accesskey;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class PjbProcessAccessKeyPolicy {

    private static final Duration MAXIMUM_VALIDITY = Duration.ofDays(90);

    public PjbProcessAccessKeyDecision evaluate(PjbProcessAccessKeyGrant grant,
                                                PjbProcessAccessKeyScope requestedScope,
                                                Instant now) {
        List<String> reasons = new ArrayList<>();
        if (grant == null) {
            return denied("MISSING_GRANT", List.of("access key grant is required"));
        }
        if (grant.revoked()) {
            reasons.add("revoked access key");
        }
        if (grant.expiredAt(now)) {
            reasons.add("expired access key");
        }
        if (!grant.hasScope(requestedScope)) {
            reasons.add("scope not granted");
        }
        if (grant.sealedCase() && requestedScope != PjbProcessAccessKeyScope.RESPONSE_TO_NOTICE) {
            reasons.add("sealed case requires scoped response channel");
        }
        if (validityExceeded(grant)) {
            reasons.add("validity exceeds policy window");
        }
        return reasons.isEmpty() ? new PjbProcessAccessKeyDecision(true, "ACCESS_ALLOWED", List.of()) : denied("ACCESS_DENIED", reasons);
    }

    public boolean validityExceeded(PjbProcessAccessKeyGrant grant) {
        if (grant == null) {
            return true;
        }
        return Duration.between(grant.issuedAt(), grant.expiresAt()).compareTo(MAXIMUM_VALIDITY) > 0;
    }

    private PjbProcessAccessKeyDecision denied(String code, List<String> reasons) {
        return new PjbProcessAccessKeyDecision(false, code, reasons);
    }
}
