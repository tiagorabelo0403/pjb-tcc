package com.tcc.pjb.backend.integration.judicial.security;

import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import java.security.KeyStore;
import java.util.Map;

public record JudicialKeyStoreMaterial(
        String reference,
        String type,
        String providerName,
        KeyStore keyStore,
        char[] storePassword,
        char[] keyPassword,
        String preferredAlias,
        boolean hardwareBacked,
        Map<String, Object> metadata
) {
    public JudicialKeyStoreMaterial {
        storePassword = storePassword == null ? null : storePassword.clone();
        keyPassword = keyPassword == null ? null : keyPassword.clone();
        metadata = JudicialMapSupport.copyNonNull(metadata);
    }

    public char[] storePasswordCopy() {
        return storePassword == null ? null : storePassword.clone();
    }

    public char[] keyPasswordCopy() {
        return keyPassword == null ? null : keyPassword.clone();
    }
}
