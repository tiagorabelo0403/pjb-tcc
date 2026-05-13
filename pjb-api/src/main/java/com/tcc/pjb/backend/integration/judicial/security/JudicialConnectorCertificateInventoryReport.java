package com.tcc.pjb.backend.integration.judicial.security;

import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialConnectorCertificateInventoryReport(
        Instant lastValidatedAt,
        JudicialSystem system,
        String tribunalCodigo,
        String environmentName,
        String bindingId,
        String targetUri,
        String keyStoreRef,
        String trustStoreRef,
        String keyAlias,
        String tlsMode,
        String validationStatus,
        boolean certificatePresent,
        boolean hardwareBacked,
        boolean validNow,
        boolean expiresSoon,
        boolean expired,
        boolean trustStorePresent,
        boolean pathValidationSucceeded,
        boolean revocationAttempted,
        boolean revocationSoftFailed,
        boolean revocationHardFailed,
        Instant notBefore,
        Instant notAfter,
        Long remainingValiditySeconds,
        Integer certificateChainLength,
        String subject,
        String issuer,
        String serialNumberHex,
        String sha256Fingerprint,
        List<String> blockers,
        List<String> warnings,
        Map<String, Object> metadata
) {
    public JudicialConnectorCertificateInventoryReport {
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        metadata = JudicialMapSupport.copyNonNull(metadata);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("lastValidatedAt", lastValidatedAt != null ? lastValidatedAt.toString() : null);
        out.put("system", system != null ? system.name() : null);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("environmentName", environmentName);
        out.put("bindingId", bindingId);
        out.put("targetUri", targetUri);
        out.put("keyStoreRef", keyStoreRef);
        out.put("trustStoreRef", trustStoreRef);
        out.put("keyAlias", keyAlias);
        out.put("tlsMode", tlsMode);
        out.put("validationStatus", validationStatus);
        out.put("certificatePresent", certificatePresent);
        out.put("hardwareBacked", hardwareBacked);
        out.put("validNow", validNow);
        out.put("expiresSoon", expiresSoon);
        out.put("expired", expired);
        out.put("trustStorePresent", trustStorePresent);
        out.put("pathValidationSucceeded", pathValidationSucceeded);
        out.put("revocationAttempted", revocationAttempted);
        out.put("revocationSoftFailed", revocationSoftFailed);
        out.put("revocationHardFailed", revocationHardFailed);
        out.put("notBefore", notBefore != null ? notBefore.toString() : null);
        out.put("notAfter", notAfter != null ? notAfter.toString() : null);
        out.put("remainingValiditySeconds", remainingValiditySeconds);
        out.put("certificateChainLength", certificateChainLength);
        out.put("subject", subject);
        out.put("issuer", issuer);
        out.put("serialNumberHex", serialNumberHex);
        out.put("sha256Fingerprint", sha256Fingerprint);
        out.put("blockers", blockers);
        out.put("warnings", warnings);
        out.put("metadata", metadata);
        out.entrySet().removeIf(entry -> entry.getValue() == null);
        return Map.copyOf(out);
    }
}
