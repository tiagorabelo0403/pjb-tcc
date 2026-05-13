package com.tcc.pjb.backend.core.security.device.policy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.util.AntPathMatcher;
import com.tcc.pjb.backend.core.security.device.DeviceSecurityProperties;

public final class SecurityActionCatalog {

    private final DeviceSecurityProperties props;
    private final AntPathMatcher matcher;
    private final List<DeviceSecurityProperties.ActionRule> rules;

    public SecurityActionCatalog(DeviceSecurityProperties props) {
        this(props, new AntPathMatcher());
    }

    public SecurityActionCatalog(DeviceSecurityProperties props, AntPathMatcher matcher) {
        this.props = Objects.requireNonNull(props);
        this.matcher = Objects.requireNonNull(matcher);
        this.rules = new ArrayList<>();
        if (props.getActionRules() != null) {
            this.rules.addAll(props.getActionRules());
        }
        this.rules.sort(Comparator.comparingInt(DeviceSecurityProperties.ActionRule::getOrder));
    }

    public SecurityActionDecision resolve(String method, String path) {
        String p = path == null ? "" : path;
        String m = method == null ? "" : method;

        for (DeviceSecurityProperties.ActionRule r : rules) {
            if (r == null) continue;
            String pattern = r.getPattern();
            if (pattern == null || pattern.isBlank()) continue;
            if (!matcher.match(pattern.trim(), p)) continue;
            if (!matchesMethod(r.getMethods(), m)) continue;
            return SecurityActionDecision.of(SecurityAction.parseOrUnknown(r.getAction()), normalizeId(r.getId()));
        }

        SecurityAction heuristicallyResolved = heuristicallyResolve(m, p);
        if (heuristicallyResolved != SecurityAction.UNKNOWN) {
            return SecurityActionDecision.of(heuristicallyResolved, "heuristic-sensitive");
        }

        if (isReadMethod(m)) {
            return SecurityActionDecision.of(SecurityAction.READ_CASE, null);
        }
        if (isWriteMethod(m)) {
            return SecurityActionDecision.of(SecurityAction.WRITE_CASE, null);
        }
        return SecurityActionDecision.of(SecurityAction.UNKNOWN, null);
    }

    public SecurityActionPolicy effectivePolicy(SecurityAction action) {
        SecurityAction a = action == null ? SecurityAction.UNKNOWN : action;

        DeviceSecurityProperties.ActionPolicy p = findPolicy(a);

        boolean deviceRequired = or(p != null ? p.getDeviceRequired() : null, true);
        boolean verifiedRequired = or(p != null ? p.getVerifiedDeviceRequired() : null, props.isRequireVerifiedDevice());
        boolean attestationRequired = or(p != null ? p.getAttestationTrustedRequired() : null, props.isRequireAttestationTrusted());
        boolean baptismRequired = or(p != null ? p.getAdvogadoBaptismRequired() : null, props.isRequireAdvogadoBaptism());

        boolean passkeyAdv = or(p != null ? p.getPasskeyRequiredForAdvogado() : null, false);
        boolean passkeyAdmin = or(p != null ? p.getPasskeyRequiredForAdmin() : null, false);

        boolean allowReadQuarantine = or(p != null ? p.getAllowReadDuringQuarantine() : null, props.isQuarantineReadOnlyEnabled());
        boolean allowWriteQuarantine = or(p != null ? p.getAllowWriteDuringQuarantine() : null, false);

        boolean justificativaRequired = or(p != null ? p.getJustificativaRequired() : null, false);

        boolean stepUpRequired = or(p != null ? p.getStepUpRequired() : null, false);
        int strongAuthMaxAgeSeconds = orInt(p != null ? p.getStrongAuthMaxAgeSeconds() : null, 0);
        boolean bindStrongAuthToDevice = or(p != null ? p.getBindStrongAuthToDevice() : null, false);
        boolean oneTimeStepUp = or(p != null ? p.getOneTimeStepUp() : null, false);
        boolean dualApprovalRequired = or(p != null ? p.getDualApprovalRequired() : null, false);
        int dualApprovalTtlSeconds = orInt(p != null ? p.getDualApprovalTtlSeconds() : null, props.getDualApprovalDefaultTtlSeconds());
        boolean auditRequired = or(p != null ? p.getAuditRequired() : null, false);
        boolean govBrRequired = or(p != null ? p.getGovBrRequired() : null, false);
        int govBrMaxAgeSeconds = orInt(p != null ? p.getGovBrMaxAgeSeconds() : null, 86400);

        if (a == SecurityAction.READ_PUBLIC) {
            deviceRequired = false;
            verifiedRequired = false;
            attestationRequired = false;
            baptismRequired = false;
            passkeyAdv = false;
            passkeyAdmin = false;
            justificativaRequired = false;
            stepUpRequired = false;
            bindStrongAuthToDevice = false;
            oneTimeStepUp = false;
            dualApprovalRequired = false;
            auditRequired = false;
            strongAuthMaxAgeSeconds = 0;
            govBrRequired = false;
            govBrMaxAgeSeconds = 0;
        }

        if (requiresJudicialHardening(a)) {
            stepUpRequired = true;
            strongAuthMaxAgeSeconds = minPositive(strongAuthMaxAgeSeconds, 180);
            bindStrongAuthToDevice = true;
            oneTimeStepUp = true;
            auditRequired = true;
            justificativaRequired = true;
        }

        if (a == SecurityAction.PUBLISH_JUDICIAL_ACT || a == SecurityAction.ISSUE_RELEASE_ORDER) {
            dualApprovalRequired = true;
            dualApprovalTtlSeconds = maxPositive(dualApprovalTtlSeconds, props.getDualApprovalDefaultTtlSeconds());
        }

        if (a == SecurityAction.ISSUE_RELEASE_ORDER) {
            passkeyAdmin = true;
            govBrRequired = true;
            govBrMaxAgeSeconds = minPositive(govBrMaxAgeSeconds, 3600);
        }

        return new SecurityActionPolicy(
                deviceRequired,
                verifiedRequired,
                attestationRequired,
                baptismRequired,
                passkeyAdv,
                passkeyAdmin,
                allowReadQuarantine,
                allowWriteQuarantine,
                justificativaRequired,
                stepUpRequired,
                strongAuthMaxAgeSeconds,
                bindStrongAuthToDevice,
                oneTimeStepUp,
                dualApprovalRequired,
                dualApprovalTtlSeconds,
                auditRequired,
                govBrRequired,
                govBrMaxAgeSeconds
        );
    }

    public boolean isReadMethod(String method) {
        if (method == null) return false;
        if (props.getReadMethods() == null) return false;
        for (String m : props.getReadMethods()) {
            if (m != null && m.equalsIgnoreCase(method)) return true;
        }
        return false;
    }

    public boolean isWriteMethod(String method) {
        if (method == null) return false;
        if (props.getEnforceMethods() == null) return false;
        for (String m : props.getEnforceMethods()) {
            if (m != null && m.equalsIgnoreCase(method)) return true;
        }
        return false;
    }

    private SecurityAction heuristicallyResolve(String method, String path) {
        if (!isWriteMethod(method)) {
            return SecurityAction.UNKNOWN;
        }
        String normalized = normalizePath(path);
        if (normalized.contains("publica") || normalized.contains("publicacao")) {
            return SecurityAction.PUBLISH_JUDICIAL_ACT;
        }
        if (normalized.contains("transito")) {
            return SecurityAction.CERTIFY_TRANSIT;
        }
        if (normalized.contains("arquiv")) {
            return SecurityAction.ARCHIVE_CASE;
        }
        if (normalized.contains("cumprimento") || normalized.contains("execucao")) {
            return SecurityAction.EXECUTE_JUDICIAL_ACT;
        }
        if (normalized.contains("alvara")) {
            return SecurityAction.ISSUE_RELEASE_ORDER;
        }
        if (normalized.contains("mandado")) {
            return SecurityAction.ISSUE_MANDATE;
        }
        if (normalized.contains("sentenca") || normalized.contains("voto") || normalized.contains("acordao") || normalized.contains("despacho") || normalized.contains("proclamacao")) {
            return SecurityAction.WRITE_JUDICIAL_ACT;
        }
        return SecurityAction.UNKNOWN;
    }

    private boolean requiresJudicialHardening(SecurityAction action) {
        return switch (action) {
            case WRITE_JUDICIAL_ACT,
                    PUBLISH_JUDICIAL_ACT,
                    CERTIFY_TRANSIT,
                    ARCHIVE_CASE,
                    EXECUTE_JUDICIAL_ACT,
                    ISSUE_MANDATE,
                    ISSUE_RELEASE_ORDER -> true;
            default -> false;
        };
    }

    private String normalizePath(String path) {
        return path == null ? "" : path.trim().toLowerCase(Locale.ROOT);
    }

    private DeviceSecurityProperties.ActionPolicy findPolicy(SecurityAction action) {
        if (props.getActionPolicies() == null) return null;
        Map<String, DeviceSecurityProperties.ActionPolicy> map = props.getActionPolicies();
        if (map.isEmpty()) return null;
        DeviceSecurityProperties.ActionPolicy p = map.get(action.name());
        if (p != null) return p;
        return map.get(action.name().toLowerCase(Locale.ROOT));
    }

    private boolean matchesMethod(List<String> methods, String method) {
        if (methods == null || methods.isEmpty()) return true;
        if (method == null || method.isBlank()) return false;
        for (String m : methods) {
            if (m != null && m.equalsIgnoreCase(method)) return true;
        }
        return false;
    }

    private static String normalizeId(String v) {
        if (v == null) return null;
        String s = v.trim();
        if (s.isEmpty()) return null;
        if (s.length() > 64) s = s.substring(0, 64);
        return s.replaceAll("[^a-zA-Z0-9_./-]", "");
    }

    private static boolean or(Boolean v, boolean fallback) {
        return v != null ? v.booleanValue() : fallback;
    }

    private static int orInt(Integer v, int fallback) {
        return v != null ? v.intValue() : fallback;
    }

    private static int minPositive(int current, int candidate) {
        if (current <= 0) {
            return candidate;
        }
        return Math.min(current, candidate);
    }

    private static int maxPositive(int current, int candidate) {
        if (current <= 0) {
            return candidate;
        }
        return Math.max(current, candidate);
    }
}
