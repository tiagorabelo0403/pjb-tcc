package com.tcc.pjb.backend.core.security.device.policy.store;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.tcc.pjb.backend.core.security.device.DeviceSecurityProperties;

public record DeviceSecurityPolicySnapshot(
        DeviceSecurityPolicyDocument document,
        List<DeviceSecurityProperties.ActionRule> rules,
        Map<String, DeviceSecurityProperties.ActionPolicy> policies,
        LocalDateTime loadedAt,
        String documentHash
) {
    public static DeviceSecurityPolicySnapshot empty() {
        return new DeviceSecurityPolicySnapshot(null, null, null, LocalDateTime.now(), null);
    }

    public boolean hasRulesOverride() {
        return rules != null && !rules.isEmpty();
    }

    public boolean hasPoliciesOverride() {
        return policies != null && !policies.isEmpty();
    }
}
