package com.tcc.pjb.backend.integration.judicial.security;

import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import java.security.Key;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class JudicialConnectorSignatureService {

    private final JudicialKeyStoreLoader keyStoreLoader;
    private final JudicialConnectorLowLevelSecurityAuditService auditService;
    private final JudicialConnectorSecurityTelemetryService telemetryService;

    public JudicialConnectorSignatureService(JudicialKeyStoreLoader keyStoreLoader,
                                            JudicialConnectorLowLevelSecurityAuditService auditService,
                                            JudicialConnectorSecurityTelemetryService telemetryService) {
        this.keyStoreLoader = Objects.requireNonNull(keyStoreLoader);
        this.auditService = Objects.requireNonNull(auditService);
        this.telemetryService = Objects.requireNonNull(telemetryService);
    }

    public JudicialSignatureResult sign(JudicialSignatureRequest request) {
        Objects.requireNonNull(request);
        if (request.payload() == null || request.payload().length == 0) {
            throw new JudicialConnectorCryptographicException("Digital signature payload is required.");
        }
        JudicialKeyStoreMaterial keyStoreMaterial = keyStoreLoader.loadKeyStore(request.keyStoreRef());
        if (keyStoreMaterial == null) {
            throw new JudicialConnectorCryptographicException("Digital signature KeyStore reference is required.");
        }
        String alias = firstNonBlank(request.keyAlias(), keyStoreMaterial.preferredAlias());
        if (alias == null) {
            throw new JudicialConnectorCryptographicException("Digital signature alias is required.");
        }
        try {
            char[] keyPassword = firstNonNull(keyStoreMaterial.keyPasswordCopy(), keyStoreMaterial.storePasswordCopy());
            Key key;
            try {
                key = keyStoreMaterial.keyStore().getKey(alias, keyPassword);
            } finally {
                clear(keyPassword);
            }
            if (!(key instanceof PrivateKey privateKey)) {
                throw new JudicialConnectorCryptographicException("Selected alias does not resolve to a private key.");
            }
            Certificate certificate = keyStoreMaterial.keyStore().getCertificate(alias);
            if (!(certificate instanceof X509Certificate x509Certificate)) {
                throw new JudicialConnectorCryptographicException("Selected alias does not resolve to an X509 certificate.");
            }
            x509Certificate.checkValidity();
            String algorithm = firstNonBlank(request.algorithm(), defaultSignatureAlgorithm(privateKey.getAlgorithm()));
            Signature signature = Signature.getInstance(algorithm);
            signature.initSign(privateKey);
            signature.update(request.payload());
            byte[] signed = signature.sign();
            LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("certificateSubject", x509Certificate.getSubjectX500Principal().getName());
            metadata.put("certificateSerial", x509Certificate.getSerialNumber().toString(16));
            metadata.put("hardwareBacked", keyStoreMaterial.hardwareBacked());
            metadata.put("signatureLength", signed.length);
            metadata.put("keyAlgorithm", privateKey.getAlgorithm());
            metadata.put("signedAt", Instant.now().toString());
            metadata.putAll(request.metadata());
            metadata.entrySet().removeIf(entry -> entry.getValue() == null);
            telemetryService.recordSignature(request.system(), request.tribunalCodigo(), "SUCCESS", keyStoreMaterial.hardwareBacked());
            return new JudicialSignatureResult(
                    request.system(),
                    request.tribunalCodigo(),
                    alias,
                    algorithm,
                    signed,
                    keyStoreMaterial.hardwareBacked(),
                    Instant.now(),
                    Map.copyOf(metadata)
            );
        } catch (Exception ex) {
            JudicialResolvedSecurityBinding binding = new JudicialResolvedSecurityBinding(
                    "SIGNATURE",
                    request.system(),
                    request.tribunalCodigo(),
                    null,
                    true,
                    JudicialConnectorTlsMode.MTLS,
                    request.keyStoreRef(),
                    null,
                    alias,
                    alias,
                    true,
                    true,
                    null,
                    null,
                    java.util.List.of(),
                    java.util.List.of(),
                    java.util.List.of(),
                    Map.of("signature", true)
            );
            telemetryService.recordSignature(request.system(), request.tribunalCodigo(), "FAILURE", keyStoreMaterial.hardwareBacked());
            auditService.recordSignatureFailure(request.system(), request.tribunalCodigo(), binding, ex, request.correlationId(), request.metadata());
            throw new JudicialConnectorCryptographicException("Unable to sign payload for judicial connector " + displaySystem(request.system()) + '.', ex);
        }
    }

    private String defaultSignatureAlgorithm(String keyAlgorithm) {
        String normalized = firstNonBlank(keyAlgorithm, "RSA");
        return switch (normalized.toUpperCase()) {
            case "EC", "ECDSA" -> "SHA256withECDSA";
            case "RSASSA-PSS" -> "RSASSA-PSS";
            default -> "SHA256withRSA";
        };
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private void clear(char[] value) {
        if (value != null) {
            java.util.Arrays.fill(value, '\0');
        }
    }

    private String displaySystem(JudicialSystem system) {
        return system == null ? JudicialSystem.OUTRO.name() : system.name();
    }
}
