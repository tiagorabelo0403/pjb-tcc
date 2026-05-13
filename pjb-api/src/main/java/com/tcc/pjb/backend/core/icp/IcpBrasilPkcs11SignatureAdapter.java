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
public class IcpBrasilPkcs11SignatureAdapter implements IcpBrasilSignaturePort {

    private final JudicialKeyStoreLoader judicialKeyStoreLoader;
    private final IcpBrasilSignatureProperties properties;

    public IcpBrasilPkcs11SignatureAdapter(JudicialKeyStoreLoader judicialKeyStoreLoader,
                                           IcpBrasilSignatureProperties properties) {
        this.judicialKeyStoreLoader = Objects.requireNonNull(judicialKeyStoreLoader);
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public boolean supports(String certificateType) {
        return properties.pkcs11LibPath() != null && !properties.pkcs11LibPath().isBlank()
                && "A3".equalsIgnoreCase(certificateType);
    }

    @Override
    public IcpBrasilSignatureResult signDetached(byte[] content, IcpBrasilSignatureCommand command) {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(command, "command");
        JudicialKeyStoreMaterial material = judicialKeyStoreLoader.loadKeyStore(command.keyStoreReference());
        if (!material.hardwareBacked()) {
            throw new IllegalStateException("pkcs11_keystore_not_hardware_backed");
        }
        try {
            KeyStore keyStore = material.keyStore();
            String alias = command.keyAlias() != null && !command.keyAlias().isBlank() ? command.keyAlias().trim() : material.preferredAlias();
            if (alias == null || alias.isBlank()) {
                var aliases = keyStore.aliases();
                alias = aliases.hasMoreElements() ? aliases.nextElement() : null;
            }
            Key key = keyStore.getKey(alias, command.keyPasswordCopy() != null ? command.keyPasswordCopy() : material.keyPasswordCopy());
            if (!(key instanceof PrivateKey privateKey)) {
                throw new IllegalStateException("pkcs11_private_key_not_found");
            }
            Certificate certificate = keyStore.getCertificate(alias);
            if (!(certificate instanceof X509Certificate signerCertificate)) {
                throw new IllegalStateException("pkcs11_signer_certificate_not_found");
            }
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(content);
            return new IcpBrasilSignatureResult(
                    signature.sign(),
                    signerCertificate,
                    command.profileCandidate(),
                    "T",
                    false,
                    false,
                    false,
                    signature.getAlgorithm()
            );
        } catch (Exception ex) {
            throw new IllegalStateException("pkcs11_signature_failed", ex);
        }
    }
}
