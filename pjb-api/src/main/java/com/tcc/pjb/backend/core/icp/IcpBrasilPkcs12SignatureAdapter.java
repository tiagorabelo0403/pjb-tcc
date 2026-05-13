package com.tcc.pjb.backend.core.icp;

import com.tcc.pjb.backend.core.icp.domain.IcpBrasilSignatureCommand;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilSignatureResult;
import com.tcc.pjb.backend.integration.judicial.security.JudicialKeyStoreLoader;
import com.tcc.pjb.backend.integration.judicial.security.JudicialKeyStoreMaterial;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class IcpBrasilPkcs12SignatureAdapter implements IcpBrasilSignaturePort {

    private final JudicialKeyStoreLoader judicialKeyStoreLoader;

    public IcpBrasilPkcs12SignatureAdapter(JudicialKeyStoreLoader judicialKeyStoreLoader) {
        this.judicialKeyStoreLoader = Objects.requireNonNull(judicialKeyStoreLoader);
    }

    @Override
    public boolean supports(String certificateType) {
        String normalized = normalize(certificateType);
        return normalized == null || normalized.equals("A1") || normalized.equals("A4");
    }

    @Override
    public IcpBrasilSignatureResult signDetached(byte[] content, IcpBrasilSignatureCommand command) {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(command, "command");
        JudicialKeyStoreMaterial material = judicialKeyStoreLoader.loadKeyStore(command.keyStoreReference());
        try {
            KeyStore keyStore = material.keyStore();
            String alias = firstNonBlank(command.keyAlias(), material.preferredAlias(), firstAlias(keyStore));
            char[] password = command.keyPasswordCopy() != null ? command.keyPasswordCopy() : material.keyPasswordCopy();
            Key key = keyStore.getKey(alias, password);
            if (!(key instanceof PrivateKey privateKey)) {
                throw new IllegalStateException("private_key_not_found_for_alias");
            }
            Certificate certificate = keyStore.getCertificate(alias);
            if (!(certificate instanceof X509Certificate signerCertificate)) {
                throw new IllegalStateException("x509_certificate_not_found_for_alias");
            }
            Signature signature = Signature.getInstance(resolveAlgorithm(privateKey));
            signature.initSign(privateKey);
            signature.update(content);
            return new IcpBrasilSignatureResult(
                    signature.sign(),
                    signerCertificate,
                    command.profileCandidate(),
                    "B",
                    false,
                    false,
                    false,
                    signature.getAlgorithm()
            );
        } catch (Exception ex) {
            throw new IllegalStateException("pkcs12_signature_failed", ex);
        }
    }

    private String resolveAlgorithm(PrivateKey privateKey) {
        String algorithm = normalize(privateKey.getAlgorithm());
        if ("EC".equals(algorithm) || "ECDSA".equals(algorithm)) {
            return "SHA256withECDSA";
        }
        return "SHA256withRSA";
    }

    private String firstAlias(KeyStore keyStore) throws Exception {
        var aliases = keyStore.aliases();
        return aliases.hasMoreElements() ? aliases.nextElement() : null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = normalize(value);
            if (normalized != null) {
                return value.trim();
            }
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String out = value.trim();
        return out.isBlank() ? null : out.toUpperCase();
    }
}
