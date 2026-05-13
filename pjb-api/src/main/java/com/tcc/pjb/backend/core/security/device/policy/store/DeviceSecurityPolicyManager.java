package com.tcc.pjb.backend.core.security.device.policy.store;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.device.DeviceSecurityProperties;
import com.tcc.pjb.backend.core.security.device.policy.SecurityAction;
import com.tcc.pjb.backend.core.security.device.policy.SecurityActionCatalog;
import com.tcc.pjb.backend.core.security.device.policy.SecurityActionDecision;
import com.tcc.pjb.backend.core.security.device.policy.SecurityActionPolicy;

@Service
public class DeviceSecurityPolicyManager {

    private final DeviceSecurityProperties props;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final AtomicReference<DeviceSecurityPolicySnapshot> snapshotRef = new AtomicReference<>(DeviceSecurityPolicySnapshot.empty());

    public DeviceSecurityPolicyManager(DeviceSecurityProperties props,
                                      ResourceLoader resourceLoader,
                                      ObjectMapper objectMapper) {
        this.props = Objects.requireNonNull(props);
        this.resourceLoader = Objects.requireNonNull(resourceLoader);
        ObjectMapper mapper = Objects.requireNonNull(objectMapper).copy();
        mapper.setConfig(mapper.getSerializationConfig().with(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY));
        mapper.setConfig(mapper.getDeserializationConfig().with(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY));
        this.objectMapper = mapper;
        reloadNow();
    }

    public DeviceSecurityPolicySnapshot snapshot() {
        maybeReload();
        return snapshotRef.get();
    }

    public SecurityActionDecision resolve(String method, String path, SecurityActionCatalog fallbackCatalog) {
        DeviceSecurityPolicySnapshot snap = snapshot();
        List<DeviceSecurityProperties.ActionRule> baseRules = props.getActionRules();
        List<DeviceSecurityProperties.ActionRule> rules = snap != null && snap.hasRulesOverride() ? snap.rules() : baseRules;
        if (rules == null || rules.isEmpty()) {
            return fallbackCatalog.resolve(method, path);
        }

        String p = path == null ? "" : path;
        String m = method == null ? "" : method;

        List<DeviceSecurityProperties.ActionRule> ordered = rules.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(DeviceSecurityProperties.ActionRule::getOrder))
                .toList();

        for (DeviceSecurityProperties.ActionRule r : ordered) {
            String pattern = r.getPattern();
            if (pattern == null || pattern.isBlank()) continue;
            if (!matcher.match(pattern.trim(), p)) continue;
            if (!matchesMethod(r.getMethods(), m)) continue;
            return SecurityActionDecision.of(SecurityAction.parseOrUnknown(r.getAction()), normalizeId(r.getId()));
        }

        return fallbackCatalog.resolve(method, path);
    }

    public SecurityActionPolicy effectivePolicy(SecurityAction action, SecurityActionCatalog fallbackCatalog) {
        SecurityActionPolicy base = fallbackCatalog.effectivePolicy(action);
        DeviceSecurityProperties.ActionPolicy override = findOverridePolicy(action);
        if (override == null) return base;

        boolean deviceRequired = override.getDeviceRequired() != null ? override.getDeviceRequired() : base.deviceRequired();
        boolean verifiedRequired = override.getVerifiedDeviceRequired() != null ? override.getVerifiedDeviceRequired() : base.verifiedDeviceRequired();
        boolean attestationRequired = override.getAttestationTrustedRequired() != null ? override.getAttestationTrustedRequired() : base.attestationTrustedRequired();
        boolean baptismRequired = override.getAdvogadoBaptismRequired() != null ? override.getAdvogadoBaptismRequired() : base.advogadoBaptismRequired();
        boolean passkeyAdv = override.getPasskeyRequiredForAdvogado() != null ? override.getPasskeyRequiredForAdvogado() : base.passkeyRequiredForAdvogado();
        boolean passkeyAdmin = override.getPasskeyRequiredForAdmin() != null ? override.getPasskeyRequiredForAdmin() : base.passkeyRequiredForAdmin();
        boolean allowReadQuarantine = override.getAllowReadDuringQuarantine() != null ? override.getAllowReadDuringQuarantine() : base.allowReadDuringQuarantine();
        boolean allowWriteQuarantine = override.getAllowWriteDuringQuarantine() != null ? override.getAllowWriteDuringQuarantine() : base.allowWriteDuringQuarantine();
        boolean justificativaRequired = override.getJustificativaRequired() != null ? override.getJustificativaRequired() : base.justificativaRequired();
        boolean stepUpRequired = override.getStepUpRequired() != null ? override.getStepUpRequired() : base.stepUpRequired();
        int strongAuthMaxAgeSeconds = override.getStrongAuthMaxAgeSeconds() != null ? override.getStrongAuthMaxAgeSeconds() : base.strongAuthMaxAgeSeconds();
        boolean bindStrongAuthToDevice = override.getBindStrongAuthToDevice() != null ? override.getBindStrongAuthToDevice() : base.bindStrongAuthToDevice();
        boolean oneTimeStepUp = override.getOneTimeStepUp() != null ? override.getOneTimeStepUp() : base.oneTimeStepUp();
        boolean dualApprovalRequired = override.getDualApprovalRequired() != null ? override.getDualApprovalRequired() : base.dualApprovalRequired();
        int dualApprovalTtlSeconds = override.getDualApprovalTtlSeconds() != null ? override.getDualApprovalTtlSeconds() : base.dualApprovalTtlSeconds();
        boolean auditRequired = override.getAuditRequired() != null ? override.getAuditRequired() : base.auditRequired();
        boolean govBrRequired = override.getGovBrRequired() != null ? override.getGovBrRequired() : base.govBrRequired();
        int govBrMaxAgeSeconds = override.getGovBrMaxAgeSeconds() != null ? override.getGovBrMaxAgeSeconds() : base.govBrMaxAgeSeconds();

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

    public void reloadNow() {
        snapshotRef.set(loadSnapshot(snapshotRef.get()));
    }

    private void maybeReload() {
        if (!props.isPolicyDocumentEnabled()) return;
        int sec = props.getPolicyDocumentReloadIntervalSeconds();
        if (sec <= 0) return;
        DeviceSecurityPolicySnapshot cur = snapshotRef.get();
        if (cur == null || cur.loadedAt() == null) {
            snapshotRef.set(loadSnapshot(cur));
            return;
        }
        if (Duration.between(cur.loadedAt(), LocalDateTime.now()).getSeconds() >= sec) {
            snapshotRef.set(loadSnapshot(cur));
        }
    }

    private DeviceSecurityPolicySnapshot loadSnapshot(DeviceSecurityPolicySnapshot current) {
        DeviceSecurityPolicySnapshot fallback = current != null ? current : DeviceSecurityPolicySnapshot.empty();
        if (!props.isPolicyDocumentEnabled()) {
            return fallback;
        }

        String src = props.getPolicyDocumentSource();
        if (src == null || src.isBlank()) {
            return fallback;
        }

        try {
            Resource res = resourceLoader.getResource(src.trim());
            if (!res.exists()) {
                return fallback;
            }
            try (InputStream in = res.getInputStream()) {
                DeviceSecurityPolicyDocument doc = objectMapper.readValue(in, DeviceSecurityPolicyDocument.class);
                verifySignatureIfRequired(doc);
                String docHash = sha256Hex(canonicalBytes(doc));

                var rules = (doc != null && doc.actionRules() != null && !doc.actionRules().isEmpty()) ? doc.actionRules() : null;
                var policies = (doc != null && doc.actionPolicies() != null && !doc.actionPolicies().isEmpty()) ? doc.actionPolicies() : null;
                return new DeviceSecurityPolicySnapshot(
                        doc,
                        rules,
                        policies,
                        LocalDateTime.now(),
                        docHash
                );
            }
        } catch (Exception e) {
            return fallback;
        }
    }

    private void verifySignatureIfRequired(DeviceSecurityPolicyDocument doc) {
        if (!props.isPolicyDocumentRequireSignature()) return;
        if (doc == null) throw new IllegalArgumentException("policy doc inválido");
        String sig = doc.signature();
        if (sig == null || sig.isBlank()) {
            throw new IllegalArgumentException("policy doc sem assinatura");
        }
        String key = props.getPolicyDocumentHmacKey();
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("policy hmac key ausente");
        }
        String expected = hmacSha256Hex(key, canonicalBytes(doc));
        if (!expected.equalsIgnoreCase(sig.trim())) {
            throw new IllegalArgumentException("policy doc assinatura inválida");
        }
    }

    private byte[] canonicalBytes(DeviceSecurityPolicyDocument doc) {
        try {
            DeviceSecurityPolicyDocument unsigned = new DeviceSecurityPolicyDocument(
                    doc != null ? doc.version() : 0,
                    doc != null ? doc.issuedAt() : null,
                    doc != null ? doc.actionRules() : null,
                    doc != null ? doc.actionPolicies() : null,
                    null
            );
            return objectMapper.writeValueAsBytes(unsigned);
        } catch (Exception e) {
            throw new IllegalStateException("canonical policy falhou", e);
        }
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(bytes);
            return HexFormat.of().formatHex(dig);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    private static String hmacSha256Hex(String key, byte[] message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(message));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC indisponível", e);
        }
    }

    private DeviceSecurityProperties.ActionPolicy findOverridePolicy(SecurityAction action) {
        DeviceSecurityPolicySnapshot snap = snapshot();
        if (action == null || snap == null || !snap.hasPoliciesOverride()) return null;
        Map<String, DeviceSecurityProperties.ActionPolicy> map = snap.policies();
        if (map == null || map.isEmpty()) return null;
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
}
