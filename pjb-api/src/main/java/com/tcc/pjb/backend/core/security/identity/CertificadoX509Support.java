package com.tcc.pjb.backend.core.security.identity;

import com.tcc.pjb.backend.core.util.Hashes;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Suporte local para desserializacao e fingerprint de certificados X.509 no login por certificado.
 * Existem parsers privados semelhantes em AdvogadoBaptismService, EdgeAttestationService e
 * QualifiedSignatureCertificateContextSupport; a consolidacao em util unico fica como divida futura.
 */
@Service
public class CertificadoX509Support {

    public X509Certificate parse(String certificado) {
        if (certificado == null || certificado.isBlank()) {
            throw new IllegalArgumentException("certificado_obrigatorio");
        }
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(bytes(certificado)));
        } catch (CertificateException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("certificado_invalido", ex);
        }
    }

    public String fingerprintSha256(X509Certificate certificado) {
        try {
            return Hashes.sha256Hex(certificado.getEncoded());
        } catch (CertificateEncodingException ex) {
            throw new IllegalArgumentException("certificado_invalido", ex);
        }
    }

    public Optional<String> algoritmoAssinaturaPadrao(X509Certificate certificado) {
        if (certificado.getPublicKey() instanceof RSAPublicKey) {
            return Optional.of("SHA256withRSA");
        }
        if (certificado.getPublicKey() instanceof ECPublicKey) {
            return Optional.of("SHA256withECDSA");
        }
        return Optional.empty();
    }

    private static byte[] bytes(String certificado) {
        String normalizado = certificado
                .trim()
                .replace("\\n", "\n")
                .replace("\r", "");
        if (normalizado.contains("-----BEGIN CERTIFICATE-----")) {
            return normalizado.getBytes(StandardCharsets.UTF_8);
        }
        return Base64.getMimeDecoder().decode(normalizado);
    }
}
