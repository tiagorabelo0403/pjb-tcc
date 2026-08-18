package com.tcc.pjb.backend.core.security.identity;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "pjb.security.auth.certificado")
public record CertificadoAuthPolicy(
        @DefaultValue("120s") Duration nonceTtl,
        @DefaultValue("true") boolean enforceIcpRealEmProd
) {

    public CertificadoAuthPolicy {
        if (nonceTtl == null || nonceTtl.isZero() || nonceTtl.isNegative()) {
            throw new IllegalArgumentException("nonce_ttl_deve_ser_positivo");
        }
    }
}
