package com.tcc.pjb.backend.service.processual.recursal.pdf;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.recursal.tsa")
public record RecursalTimestampAuthorityProperties(
        Boolean enabled,
        Boolean allowEphemeralFallback,
        String keyStoreRef,
        String keyAlias,
        String policyOid,
        String authorityName,
        String profile,
        Boolean includeCertificates
) {
    public RecursalTimestampAuthorityProperties {
        enabled = enabled == null ? Boolean.TRUE : enabled;
        allowEphemeralFallback = allowEphemeralFallback == null ? Boolean.TRUE : allowEphemeralFallback;
        policyOid = policyOid == null || policyOid.isBlank() ? "1.2.840.113549.1.9.16.1.4" : policyOid.trim();
        authorityName = authorityName == null || authorityName.isBlank() ? "PJB TSA" : authorityName.trim();
        profile = profile == null || profile.isBlank() ? "RFC3161_INTERNAL" : profile.trim();
        includeCertificates = includeCertificates == null ? Boolean.TRUE : includeCertificates;
    }
}
