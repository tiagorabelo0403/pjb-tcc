package com.tcc.pjb.backend.core.security.device.policy.store;

import java.util.List;
import java.util.Map;
import com.tcc.pjb.backend.core.security.device.DeviceSecurityProperties;

public record DeviceSecurityPolicyDocument(
        int version,
        String issuedAt,
        List<DeviceSecurityProperties.ActionRule> actionRules,
        Map<String, DeviceSecurityProperties.ActionPolicy> actionPolicies,
        String signature
) {
}
