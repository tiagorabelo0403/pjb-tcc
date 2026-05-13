package com.tcc.pjb.backend.platform.security.ratelimit;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import com.tcc.pjb.backend.platform.security.rbac.CapabilityStrings;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;

public final class CapabilityRateLimitPolicyResolver {

    private final CapabilityRateLimitProperties props;

    public CapabilityRateLimitPolicyResolver(CapabilityRateLimitProperties props) {
        this.props = Objects.requireNonNull(props, "props");
    }

    public int resolveWindowSeconds() {
        return Math.max(1, props.getWindowSeconds());
    }

    
    public int resolveLimitTokens(CapabilityRateLimitDomain domain, String capabilityRaw, ApiVersion version) {
        ApiVersion v = (version != null) ? version : ApiVersion.latest();
        int globalVersionDefault = resolveGlobalVersionDefault(v);

        CapabilityRateLimitProperties.DomainPolicy dp = props.domainPolicy(domain);
        int domainBase = globalVersionDefault;
        if (dp != null && dp.getDefaultLimitTokens() != null && dp.getDefaultLimitTokens() > 0) {
            domainBase = dp.getDefaultLimitTokens();
        }

        String cap = CapabilityStrings.canonical(capabilityRaw);
        if (cap == null) return Math.max(1, domainBase);

        
        int capabilityDefault = resolveCapabilityDefault(dp, cap, domainBase);

        
        Integer specific = resolveSpecificForVersion(dp, cap, v);
        if (specific != null && specific > 0) return specific;

        return Math.max(1, capabilityDefault);
    }

    private int resolveGlobalVersionDefault(ApiVersion v) {
        int base = Math.max(1, props.getDefaultLimitTokens());
        CapabilityRateLimitProperties.VersionLimitDefaults vd = props.getVersionDefaultLimitTokens();
        if (vd == null) return base;
        Integer configured = switch (v) {
            case V1 -> vd.getV1();
            case V2 -> vd.getV2();
            case V3 -> vd.getV3();
        };
        if (configured != null && configured > 0) return configured;
        return base;
    }

    private int resolveCapabilityDefault(CapabilityRateLimitProperties.DomainPolicy dp, String cap, int base) {
        if (dp == null) return base;
        Integer exact = safeGet(dp.getExact(), cap);
        if (exact != null && exact > 0) return exact;

        Integer pref = matchLongestPrefix(dp.getPrefix(), cap);
        if (pref != null && pref > 0) return pref;

        return base;
    }

    private Integer resolveSpecificForVersion(CapabilityRateLimitProperties.DomainPolicy dp, String cap, ApiVersion v) {
        if (dp == null) return null;
        Map<String, CapabilityRateLimitProperties.VersionPolicy> versions = dp.getVersions();
        if (versions == null || versions.isEmpty()) return null;

        
        CapabilityRateLimitProperties.VersionPolicy vp = versions.get(v.canonical());
        if (vp == null) vp = versions.get(v.name());
        if (vp == null) {
            String keyLower = v.name().toLowerCase(java.util.Locale.ROOT);
            vp = versions.entrySet().stream()
                    .filter(e -> e.getKey() != null && e.getKey().trim().toLowerCase(java.util.Locale.ROOT).equals(keyLower))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
        }
        if (vp == null) return null;

        Integer exact = safeGet(vp.getExact(), cap);
        if (exact != null) return exact;

        Integer pref = matchLongestPrefix(vp.getPrefix(), cap);
        if (pref != null) return pref;

        return null;
    }

    private static Integer matchLongestPrefix(Map<String, Integer> prefixes, String cap) {
        if (prefixes == null || prefixes.isEmpty() || cap == null) return null;
        return prefixes.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getValue() != null && e.getValue() > 0)
                .map(e -> Map.entry(CapabilityStrings.canonical(e.getKey()), e.getValue()))
                .filter(e -> e.getKey() != null && cap.startsWith(e.getKey()))
                .max(Comparator.comparingInt(e -> e.getKey().length()))
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    public int resolveCostTokens(ApiVersion version) {
        CapabilityRateLimitProperties.VersionCost vc = props.getVersionCost();
        if (vc == null) {
            return 1;
        }
        ApiVersion v = version != null ? version : ApiVersion.latest();
        int cost = switch (v) {
            case V1 -> vc.getV1();
            case V2 -> vc.getV2();
            case V3 -> vc.getV3();
        };
        return Math.max(1, cost);
    }

    private static Integer safeGet(Map<String, Integer> map, String key) {
        if (map == null || map.isEmpty() || key == null) return null;
        Integer v = map.get(key);
        if (v != null) return v;
        
        String k = key.trim();
        return map.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getKey().trim().equalsIgnoreCase(k))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
