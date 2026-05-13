package com.tcc.pjb.backend.core.icp;

import com.tcc.pjb.backend.integration.judicial.security.JudicialKeyStoreLoader;
import com.tcc.pjb.backend.integration.judicial.security.JudicialKeyStoreMaterial;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Objects;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class IcpBrasilSignerCertificateResolver {

    private final JudicialKeyStoreLoader judicialKeyStoreLoader;
    private final IcpBrasilSignatureProperties properties;

    public IcpBrasilSignerCertificateResolver(JudicialKeyStoreLoader judicialKeyStoreLoader,
                                              IcpBrasilSignatureProperties properties) {
        this.judicialKeyStoreLoader = Objects.requireNonNull(judicialKeyStoreLoader);
        this.properties = Objects.requireNonNull(properties);
    }

    @Nullable
    public X509Certificate resolveRecursalCertificate() {
        String keyStoreRef = blankToNull(properties.recursalKeyStoreRef());
        if (keyStoreRef == null) {
            return null;
        }
        JudicialKeyStoreMaterial material = judicialKeyStoreLoader.loadKeyStore(keyStoreRef);
        if (material == null || material.keyStore() == null) {
            return null;
        }
        try {
            KeyStore keyStore = material.keyStore();
            String alias = blankToNull(properties.recursalKeyAlias());
            if (alias == null) {
                alias = blankToNull(material.preferredAlias());
            }
            if (alias == null) {
                var aliases = keyStore.aliases();
                if (!aliases.hasMoreElements()) {
                    return null;
                }
                alias = aliases.nextElement();
            }
            Certificate certificate = keyStore.getCertificate(alias);
            return certificate instanceof X509Certificate x509Certificate ? x509Certificate : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String out = value.trim();
        return out.isBlank() ? null : out;
    }
}
