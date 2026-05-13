package com.tcc.pjb.backend.core.icp;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.icp")
public record IcpBrasilSignatureProperties(
        boolean enabled,
        boolean enforceChainValidation,
        List<String> acceptedAcSiglas,
        String ocspCacheKeyPrefix,
        long ocspCacheTtlSeconds,
        long crlCacheTtlSeconds,
        String pkcs11LibPath,
        String recursalKeyStoreRef,
        String recursalKeyAlias,
        String recursalProfileCandidate,
        boolean recursalRequireCertificate
) {
}
