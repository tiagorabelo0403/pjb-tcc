package com.tcc.pjb.backend.integration.judicial.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.model.entity.judicial.JudicialConnectorCryptographicFailureEvent;
import com.tcc.pjb.backend.model.repository.JudicialConnectorCryptographicFailureEventRepository;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLProtocolException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JudicialConnectorLowLevelSecurityAuditService {

    private final JudicialConnectorCryptographicFailureEventRepository repository;
    private final ObjectMapper objectMapper;
    private final JudicialConnectorSecurityProperties properties;
    private final JudicialConnectorSecurityTelemetryService telemetryService;

    public JudicialConnectorLowLevelSecurityAuditService(JudicialConnectorCryptographicFailureEventRepository repository,
                                                         ObjectMapper objectMapper,
                                                         JudicialConnectorSecurityProperties properties,
                                                         JudicialConnectorSecurityTelemetryService telemetryService) {
        this.repository = Objects.requireNonNull(repository);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.properties = Objects.requireNonNull(properties);
        this.telemetryService = Objects.requireNonNull(telemetryService);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordContextFailure(JudicialSystem system,
                                     String tribunalCodigo,
                                     URI targetUri,
                                     String operationName,
                                     JudicialResolvedSecurityBinding binding,
                                     Throwable failure,
                                     String correlationId,
                                     Map<String, Object> metadata) {
        persist(system, tribunalCodigo, targetUri, operationName, binding, failure, correlationId, metadata);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordTransportFailure(JudicialSystem system,
                                       String tribunalCodigo,
                                       URI targetUri,
                                       String operationName,
                                       JudicialResolvedSecurityBinding binding,
                                       Throwable failure,
                                       String correlationId,
                                       Map<String, Object> metadata) {
        persist(system, tribunalCodigo, targetUri, operationName, binding, failure, correlationId, metadata);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSignatureFailure(JudicialSystem system,
                                       String tribunalCodigo,
                                       JudicialResolvedSecurityBinding binding,
                                       Throwable failure,
                                       String correlationId,
                                       Map<String, Object> metadata) {
        persist(system, tribunalCodigo, null, "DIGITAL_SIGNATURE", binding, failure, correlationId, metadata);
    }

    private void persist(JudicialSystem system,
                         String tribunalCodigo,
                         URI targetUri,
                         String operationName,
                         JudicialResolvedSecurityBinding binding,
                         Throwable failure,
                         String correlationId,
                         Map<String, Object> metadata) {
        Throwable root = rootCause(failure);
        JudicialConnectorCryptographicFailureType failureType = classify(root);
        String sanitizedMessage = sanitize(root);
        String hostSha256 = targetUri == null || targetUri.getHost() == null ? null : sha256(targetUri.getHost().toLowerCase());
        String fingerprint = sha256(String.join(
                "|",
                system != null ? system.name() : JudicialSystem.OUTRO.name(),
                tribunalCodigo == null ? "" : tribunalCodigo,
                operationName == null ? "" : operationName,
                failureType.name(),
                sanitizedMessage,
                hostSha256 == null ? "" : hostSha256,
                binding != null && binding.tlsMode() != null ? binding.tlsMode().name() : ""
        ));
        LinkedHashMap<String, Object> compactMetadata = new LinkedHashMap<>();
        compactMetadata.put("environmentName", binding != null ? binding.environmentName() : properties.getEnvironmentName());
        compactMetadata.put("bindingId", binding != null ? binding.bindingId() : null);
        compactMetadata.put("targetScheme", targetUri != null ? targetUri.getScheme() : null);
        compactMetadata.put("targetPort", targetUri != null && targetUri.getPort() >= 0 ? targetUri.getPort() : null);
        compactMetadata.put("failureClass", root.getClass().getName());
        compactMetadata.put("messageDigest", sha256(sanitizedMessage));
        if (metadata != null && !metadata.isEmpty()) {
            compactMetadata.putAll(metadata);
        }
        compactMetadata.entrySet().removeIf(entry -> entry.getValue() == null);
        JudicialConnectorCryptographicFailureEvent event = new JudicialConnectorCryptographicFailureEvent();
        event.setConnectorSystem(system);
        event.setTribunalCodigo(tribunalCodigo);
        event.setEnvironmentName(binding != null && binding.environmentName() != null ? binding.environmentName() : properties.getEnvironmentName());
        event.setOperationName(truncate(operationName, 100));
        event.setTargetScheme(targetUri != null ? truncate(targetUri.getScheme(), 20) : null);
        event.setTargetHostSha256(hostSha256);
        event.setTargetPort(targetUri != null && targetUri.getPort() >= 0 ? targetUri.getPort() : null);
        event.setFailureType(failureType);
        event.setFailureCode(failureType.name());
        event.setFailureFingerprint(fingerprint);
        event.setSanitizedMessage(truncate(sanitizedMessage, 1000));
        event.setKeyAlias(binding != null ? truncate(binding.keyAlias(), 255) : null);
        event.setKeyStoreRef(binding != null ? truncate(binding.keyStoreRef(), 160) : null);
        event.setTrustStoreRef(binding != null ? truncate(binding.trustStoreRef(), 160) : null);
        event.setTlsMode(binding != null && binding.tlsMode() != null ? binding.tlsMode().name() : null);
        event.setCorrelationId(truncate(correlationId, 200));
        event.setMetadataJson(writeJson(compactMetadata));
        repository.save(event);
        telemetryService.recordCryptographicFailure(system, tribunalCodigo, failureType, operationName);
    }

    private JudicialConnectorCryptographicFailureType classify(Throwable failure) {
        List<Throwable> chain = chain(failure);
        if (contains(chain, SSLHandshakeException.class)) {
            return JudicialConnectorCryptographicFailureType.TLS_HANDSHAKE_FAILURE;
        }
        if (contains(chain, SSLProtocolException.class)) {
            return JudicialConnectorCryptographicFailureType.TLS_PROTOCOL_FAILURE;
        }
        if (contains(chain, SSLException.class)) {
            return JudicialConnectorCryptographicFailureType.TLS_IO_FAILURE;
        }
        if (contains(chain, CertificateException.class)) {
            return JudicialConnectorCryptographicFailureType.CERTIFICATE_VALIDATION_FAILURE;
        }
        if (contains(chain, UnrecoverableKeyException.class)) {
            return JudicialConnectorCryptographicFailureType.KEY_MATERIAL_ACCESS_FAILURE;
        }
        if (contains(chain, InvalidKeyException.class)) {
            return JudicialConnectorCryptographicFailureType.SIGNATURE_KEY_FAILURE;
        }
        if (contains(chain, SignatureException.class)) {
            return JudicialConnectorCryptographicFailureType.SIGNATURE_EXECUTION_FAILURE;
        }
        if (contains(chain, KeyStoreException.class)) {
            return JudicialConnectorCryptographicFailureType.KEYSTORE_LOAD_FAILURE;
        }
        if (contains(chain, NoSuchProviderException.class)) {
            return JudicialConnectorCryptographicFailureType.PKCS11_PROVIDER_FAILURE;
        }
        if (contains(chain, java.security.GeneralSecurityException.class)) {
            return JudicialConnectorCryptographicFailureType.SECURITY_CONFIGURATION_FAILURE;
        }
        if (contains(chain, IOException.class)) {
            return JudicialConnectorCryptographicFailureType.TLS_IO_FAILURE;
        }
        return JudicialConnectorCryptographicFailureType.UNCLASSIFIED_CRYPTOGRAPHIC_FAILURE;
    }

    private List<Throwable> chain(Throwable failure) {
        ArrayList<Throwable> chain = new ArrayList<>();
        Throwable current = Objects.requireNonNullElseGet(failure, () -> new IllegalStateException("Unknown cryptographic failure"));
        while (current != null && !chain.contains(current)) {
            chain.add(current);
            current = current.getCause();
        }
        return List.copyOf(chain);
    }

    private boolean contains(List<Throwable> chain, Class<?> type) {
        return chain.stream().anyMatch(type::isInstance);
    }

    private Throwable rootCause(Throwable failure) {
        List<Throwable> chain = chain(failure);
        return chain.getLast();
    }

    private String sanitize(Throwable failure) {
        String raw = failure.getClass().getSimpleName() + ": " + Objects.requireNonNullElse(failure.getMessage(), "No diagnostic message");
        return raw.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').replaceAll("\\s+", " ").trim();
    }

    private String writeJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(JudicialMapSupport.copyNonNull(metadata));
        } catch (Exception ex) {
            return "{\"serializationError\":true}";
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(Objects.requireNonNullElse(value, "").getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new JudicialConnectorCryptographicException("Unable to compute SHA-256 fingerprint.", ex);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
