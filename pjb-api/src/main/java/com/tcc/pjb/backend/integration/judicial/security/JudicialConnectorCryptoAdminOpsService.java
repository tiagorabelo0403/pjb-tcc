package com.tcc.pjb.backend.integration.judicial.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.integration.judicial.JudicialIntegrationProperties;
import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import com.tcc.pjb.backend.model.entity.judicial.JudicialConnectorAdminOperation;
import com.tcc.pjb.backend.model.repository.JudicialConnectorAdminOperationRepository;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JudicialConnectorCryptoAdminOpsService {

    private final JudicialIntegrationProperties integrationProperties;
    private final JudicialConnectorCertificateValidationService certificateValidationService;
    private final JudicialConnectorSecurityPackService securityPackService;
    private final JudicialConnectorTransport transport;
    private final JudicialConnectorAdminOperationRepository operationRepository;
    private final ObjectMapper objectMapper;

    public JudicialConnectorCryptoAdminOpsService(JudicialIntegrationProperties integrationProperties,
                                                  JudicialConnectorCertificateValidationService certificateValidationService,
                                                  JudicialConnectorSecurityPackService securityPackService,
                                                  JudicialConnectorTransport transport,
                                                  JudicialConnectorAdminOperationRepository operationRepository,
                                                  ObjectMapper objectMapper) {
        this.integrationProperties = Objects.requireNonNull(integrationProperties);
        this.certificateValidationService = Objects.requireNonNull(certificateValidationService);
        this.securityPackService = Objects.requireNonNull(securityPackService);
        this.transport = Objects.requireNonNull(transport);
        this.operationRepository = Objects.requireNonNull(operationRepository);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional
    public JudicialConnectorCryptoAdminReport inspect(JudicialConnectorCryptoProbeRequest request) {
        Objects.requireNonNull(request);
        JudicialIntegrationProperties.Connector connector = integrationProperties.connectorFor(request.system());
        URI targetUri = resolveTargetUri(request.targetUrl(), connector);
        Instant startedAt = Instant.now();
        JudicialCertificateValidationReport validation = certificateValidationService.validate(request.system(), request.tribunalCodigo(), targetUri, connector, request.metadata());
        JudicialConnectorSecurityPackReport effectivePack = securityPackService.effectivePack(request.system(), request.tribunalCodigo());
        Duration duration = Duration.between(startedAt, Instant.now());
        String outcomeStatus = "VALID".equals(validation.status()) ? "OK" : validation.blockers().isEmpty() ? "WARNINGS" : "BLOCKED";
        String outcomeMessage = switch (outcomeStatus) {
            case "OK" -> "Cryptographic material validated";
            case "WARNINGS" -> "Cryptographic material validated with warnings";
            default -> "Cryptographic material blocked";
        };
        persistOperation("CRYPTO_INSPECT", request, outcomeStatus, outcomeMessage, validation, null, duration);
        return new JudicialConnectorCryptoAdminReport(
                Instant.now(),
                "CRYPTO_INSPECT",
                request.system(),
                normalizeCode(request.tribunalCodigo()),
                targetUri != null ? targetUri.toString() : null,
                outcomeStatus,
                outcomeMessage,
                validation,
                null,
                duration,
                recentOperations(),
                JudicialMapSupport.compact("requestedBy", trim(request.requestedBy()), "effectivePack", effectivePack.toMap())
        );
    }

    @Transactional
    public JudicialConnectorCryptoAdminReport probeHandshake(JudicialConnectorCryptoProbeRequest request) {
        Objects.requireNonNull(request);
        JudicialIntegrationProperties.Connector connector = integrationProperties.connectorFor(request.system());
        URI targetUri = resolveTargetUri(request.targetUrl(), connector);
        Instant startedAt = Instant.now();
        JudicialCertificateValidationReport validation = certificateValidationService.validate(request.system(), request.tribunalCodigo(), targetUri, connector, request.metadata());
        JudicialConnectorSecurityPackReport effectivePack = securityPackService.effectivePack(request.system(), request.tribunalCodigo());
        try {
            JudicialSecureHttpResponse response = transport.exchange(
                    request.system(),
                    request.tribunalCodigo(),
                    targetUri,
                    connector,
                    JudicialSecureHttpRequest.get(
                            Map.of("Accept", List.of("*/*")),
                            Duration.ofSeconds(10),
                            "CRYPTO_PROBE_HANDSHAKE",
                            null,
                            null));
            int httpStatus = response.statusCode();
            String outcomeStatus = httpStatus >= 200 && httpStatus < 300 ? "OK" : httpStatus >= 400 && httpStatus < 500 ? "WARNINGS" : "BLOCKED";
            String outcomeMessage = switch (outcomeStatus) {
                case "OK" -> "TLS handshake and HTTP exchange successful";
                case "WARNINGS" -> "TLS handshake succeeded, HTTP response indicates client error";
                default -> "TLS handshake or HTTP exchange failed";
            };
            Duration duration = Duration.between(startedAt, Instant.now());
            persistOperation("CRYPTO_PROBE_HANDSHAKE", request, outcomeStatus, outcomeMessage, validation, httpStatus, duration);
            return new JudicialConnectorCryptoAdminReport(
                    Instant.now(),
                    "CRYPTO_PROBE_HANDSHAKE",
                    request.system(),
                    normalizeCode(request.tribunalCodigo()),
                    targetUri != null ? targetUri.toString() : null,
                    outcomeStatus,
                    outcomeMessage,
                    validation,
                    httpStatus,
                    duration,
                    recentOperations(),
                    JudicialMapSupport.compact("requestedBy", trim(request.requestedBy()), "effectivePack", effectivePack.toMap())
            );
        } catch (Exception ex) {
            Duration duration = Duration.between(startedAt, Instant.now());
            String outcomeStatus = "BLOCKED";
            String outcomeMessage = "Transport error: " + ex.getClass().getSimpleName();
            persistOperation("CRYPTO_PROBE_HANDSHAKE", request, outcomeStatus, outcomeMessage, validation, null, duration);
            return new JudicialConnectorCryptoAdminReport(
                    Instant.now(),
                    "CRYPTO_PROBE_HANDSHAKE",
                    request.system(),
                    normalizeCode(request.tribunalCodigo()),
                    targetUri != null ? targetUri.toString() : null,
                    outcomeStatus,
                    outcomeMessage,
                    validation,
                    null,
                    duration,
                    recentOperations(),
                    JudicialMapSupport.compact("requestedBy", trim(request.requestedBy()), "effectivePack", effectivePack.toMap(), "error", ex.getMessage())
            );
        }
    }

    private URI resolveTargetUri(String targetUrl, JudicialIntegrationProperties.Connector connector) {
        if (targetUrl != null && !targetUrl.isBlank()) {
            return URI.create(targetUrl.trim());
        }
        if (connector != null && connector.getBaseUrl() != null && !connector.getBaseUrl().isBlank()) {
            return URI.create(connector.getBaseUrl().trim());
        }
        return null;
    }

    private String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private void persistOperation(String operationType,
                                  JudicialConnectorCryptoProbeRequest request,
                                  String outcomeStatus,
                                  String outcomeMessage,
                                  JudicialCertificateValidationReport validation,
                                  Integer httpStatus,
                                  Duration duration) {
        try {
            String payloadJson = objectMapper.writeValueAsString(Map.of(
                    "system", request.system() != null ? request.system().name() : "",
                    "tribunalCodigo", normalizeCode(request.tribunalCodigo()) != null ? normalizeCode(request.tribunalCodigo()) : "",
                    "targetUrl", request.targetUrl() != null ? request.targetUrl() : "",
                    "validationStatus", validation != null ? validation.status() : "",
                    "httpStatus", httpStatus != null ? httpStatus : "",
                    "durationMs", duration != null ? duration.toMillis() : ""
            ));
            JudicialConnectorAdminOperation operation = new JudicialConnectorAdminOperation();
            operation.setConnectorSystem(request.system());
            operation.setTribunalCodigo(normalizeCode(request.tribunalCodigo()));
            operation.setOperationType(operationType);
            operation.setRequestedBy(trim(request.requestedBy()));
            operation.setPayloadJson(payloadJson);
            operation.setOutcomeStatus(outcomeStatus);
            operation.setOutcomeMessage(outcomeMessage);
            operationRepository.save(operation);
        } catch (Exception ignored) {
        }
    }

    public List<Map<String, Object>> recentOperations() {
        try {
            return operationRepository.findTop100ByOrderByCreatedAtDesc()
                    .stream()
                    .map(operation -> {
                        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
                        out.put("operationType", operation.getOperationType());
                        out.put("system", operation.getConnectorSystem() != null ? operation.getConnectorSystem().name() : null);
                        out.put("tribunalCodigo", operation.getTribunalCodigo());
                        out.put("outcomeStatus", operation.getOutcomeStatus());
                        out.put("outcomeMessage", operation.getOutcomeMessage());
                        out.entrySet().removeIf(entry -> entry.getValue() == null);
                        return Map.copyOf(out);
                    })
                    .toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }
}
