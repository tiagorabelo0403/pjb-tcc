package com.tcc.pjb.backend.service.processual.recursal.pdf;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.recursal.native-pdf")
public record RecursalNativePdfSignatureProperties(
        Boolean enabled,
        String keyStoreRef,
        String keyAlias,
        String reason,
        String location,
        String signerName,
        String profile
) {
    public RecursalNativePdfSignatureProperties {
        enabled = enabled == null ? Boolean.TRUE : enabled;
        reason = reason == null || reason.isBlank() ? "Formalização recursal protocolável" : reason.trim();
        location = location == null || location.isBlank() ? "PJB" : location.trim();
        signerName = signerName == null || signerName.isBlank() ? "PJB - Assinatura Recursal" : signerName.trim();
        profile = profile == null || profile.isBlank() ? "PADES_BASELINE_B" : profile.trim();
    }
}
