package com.tcc.pjb.backend.core.comunicacao.judicial.hsm;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.hsm")
public record PjbHsmProperties(
        boolean enabled,
        boolean mockEnabled,
        String pkcs11ConfigPath,
        String keyAlias,
        String pin,
        String trustStorePath,
        String trustStorePassword,
        String trustStoreType,
        String signatureAlgorithm,
        Duration operationTimeout,
        int maxConcurrentOps,
        long interceptacaoTimeoutMs,
        boolean auditarOperacoes
) {
    public PjbHsmProperties {
        signatureAlgorithm = signatureAlgorithm != null ? signatureAlgorithm : "SHA256withRSA";
        trustStoreType = trustStoreType != null ? trustStoreType : "PKCS12";
        keyAlias = keyAlias != null ? keyAlias : "aliasGovPJB";
        operationTimeout = operationTimeout != null ? operationTimeout : Duration.ofSeconds(5);
        interceptacaoTimeoutMs = interceptacaoTimeoutMs > 0 ? interceptacaoTimeoutMs : 8_000L;
        maxConcurrentOps = maxConcurrentOps > 0 ? maxConcurrentOps : 8;
    }

    public void validateIfEnabled() {
        if (!enabled || mockEnabled) {
            return;
        }
        if (pkcs11ConfigPath == null || pkcs11ConfigPath.isBlank()) {
            throw new IllegalStateException("pjb.hsm.pkcs11-config-path obrigatório quando HSM habilitado.");
        }
        if (keyAlias == null || keyAlias.isBlank()) {
            throw new IllegalStateException("pjb.hsm.key-alias obrigatório quando HSM habilitado.");
        }
        if (pin == null || pin.isBlank()) {
            throw new IllegalStateException("pjb.hsm.pin obrigatório quando HSM habilitado.");
        }
        if (trustStorePath == null || trustStorePath.isBlank()) {
            throw new IllegalStateException("pjb.hsm.trust-store-path obrigatório quando HSM habilitado.");
        }
        if (trustStorePassword == null || trustStorePassword.isBlank()) {
            throw new IllegalStateException("pjb.hsm.trust-store-password obrigatório quando HSM habilitado.");
        }
    }
}
