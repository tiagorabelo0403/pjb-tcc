package com.tcc.pjb.backend.integration.judicial.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.model.entity.judicial.JudicialConnectorSecuritySession;
import com.tcc.pjb.backend.model.repository.JudicialConnectorSecuritySessionRepository;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JudicialConnectorSecuritySessionService {

    private final JudicialConnectorSecuritySessionRepository repository;
    private final ObjectMapper objectMapper;

    public JudicialConnectorSecuritySessionService(JudicialConnectorSecuritySessionRepository repository,
                                                   ObjectMapper objectMapper) {
        this.repository = Objects.requireNonNull(repository);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(JudicialSystem system,
                              String tribunalCodigo,
                              URI targetUri,
                              JudicialConnectorCryptographicContext context,
                              JudicialSecureHttpRequest request,
                              JudicialSecureHttpResponse response,
                              Duration duration) {
        String outcomeStatus = response != null && response.statusCode() >= 500 ? "REMOTE_FAILURE" : "SUCCESS";
        persist(system, tribunalCodigo, targetUri, context, request, outcomeStatus, response != null && response.statusCode() < 500, response != null ? response.statusCode() : null, duration, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(JudicialSystem system,
                              String tribunalCodigo,
                              URI targetUri,
                              JudicialConnectorCryptographicContext context,
                              JudicialSecureHttpRequest request,
                              Duration duration,
                              Throwable failure) {
        persist(system, tribunalCodigo, targetUri, context, request, classifyFailure(failure), false, null, duration, failure);
    }

    @Transactional(readOnly = true)
    public List<JudicialConnectorSecuritySessionReport> recentSessions(Duration window, String tribunalCodigo) {
        Duration effectiveWindow = window == null || window.isNegative() || window.isZero() ? Duration.ofHours(24) : window;
        Instant threshold = Instant.now().minus(effectiveWindow);
        List<JudicialConnectorSecuritySession> sessions = normalized(tribunalCodigo) == null
                ? repository.findTop300ByCreatedAtAfterOrderByCreatedAtDesc(threshold)
                : repository.findTop300ByTribunalCodigoIgnoreCaseAndCreatedAtAfterOrderByCreatedAtDesc(normalized(tribunalCodigo), threshold);
        return sessions.stream().map(this::toReport).toList();
    }

    @Transactional(readOnly = true)
    public JudicialConnectorSecuritySessionSummary summary(Duration window, String tribunalCodigo) {
        List<JudicialConnectorSecuritySessionReport> sessions = recentSessions(window, tribunalCodigo);
        long successCount = sessions.stream().filter(JudicialConnectorSecuritySessionReport::success).count();
        long remoteFailureCount = sessions.stream().filter(report -> "REMOTE_FAILURE".equals(normalized(report.outcomeStatus()))).count();
        long transportFailureCount = sessions.stream().filter(report -> !report.success() && !"REMOTE_FAILURE".equals(normalized(report.outcomeStatus()))).count();
        long mutualTlsCount = sessions.stream().filter(JudicialConnectorSecuritySessionReport::mutualTls).count();
        long hardwareBackedCount = sessions.stream().filter(JudicialConnectorSecuritySessionReport::hardwareBacked).count();
        long hostnameVerifiedCount = sessions.stream().filter(JudicialConnectorSecuritySessionReport::hostnameVerification).count();
        long averageDurationMillis = sessions.isEmpty() ? 0L : Math.round(sessions.stream().mapToLong(JudicialConnectorSecuritySessionReport::durationMillis).average().orElse(0.0));
        long maxDurationMillis = sessions.stream().mapToLong(JudicialConnectorSecuritySessionReport::durationMillis).max().orElse(0L);
        Instant latestSessionAt = sessions.stream().map(JudicialConnectorSecuritySessionReport::createdAt).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
        LinkedHashMap<String, Long> outcomeCounts = new LinkedHashMap<>();
        sessions.forEach(report -> outcomeCounts.merge(normalized(report.outcomeStatus()), 1L, Long::sum));
        List<Map<String, Object>> outcomeBreakdown = outcomeCounts.entrySet().stream()
                .map(entry -> JudicialMapSupport.compact("outcomeStatus", entry.getKey(), "count", entry.getValue()))
                .toList();
        LinkedHashSet<String> systems = new LinkedHashSet<>();
        LinkedHashSet<String> tribunals = new LinkedHashSet<>();
        sessions.forEach(report -> {
            if (report.system() != null) {
                systems.add(report.system().name());
            }
            if (normalized(report.tribunalCodigo()) != null) {
                tribunals.add(normalized(report.tribunalCodigo()));
            }
        });
        return new JudicialConnectorSecuritySessionSummary(
                Instant.now(),
                normalized(tribunalCodigo),
                sessions.size(),
                successCount,
                remoteFailureCount,
                transportFailureCount,
                mutualTlsCount,
                hardwareBackedCount,
                hostnameVerifiedCount,
                averageDurationMillis,
                maxDurationMillis,
                latestSessionAt,
                outcomeBreakdown,
                JudicialMapSupport.compact(
                        "coveredSystems", List.copyOf(systems),
                        "coveredTribunals", List.copyOf(tribunals)
                )
        );
    }

    private void persist(JudicialSystem system,
                         String tribunalCodigo,
                         URI targetUri,
                         JudicialConnectorCryptographicContext context,
                         JudicialSecureHttpRequest request,
                         String outcomeStatus,
                         boolean success,
                         Integer httpStatusCode,
                         Duration duration,
                         Throwable failure) {
        JudicialConnectorSecuritySession session = new JudicialConnectorSecuritySession();
        session.setConnectorSystem(system == null ? JudicialSystem.OUTRO : system);
        session.setTribunalCodigo(normalized(tribunalCodigo));
        session.setEnvironmentName(context != null && context.binding() != null ? normalized(context.binding().environmentName()) : null);
        session.setOperationName(truncate(normalized(request != null ? request.operationName() : null), 120));
        session.setTargetScheme(targetUri != null ? truncate(normalized(targetUri.getScheme()), 20) : null);
        session.setTargetHostSha256(targetUri != null && normalized(targetUri.getHost()) != null ? sha256(normalized(targetUri.getHost()).toLowerCase(Locale.ROOT)) : null);
        session.setTargetPort(targetUri != null && targetUri.getPort() >= 0 ? targetUri.getPort() : null);
        session.setTlsMode(context != null && context.binding() != null && context.binding().tlsMode() != null ? context.binding().tlsMode().name() : null);
        session.setOutcomeStatus(truncate(normalized(outcomeStatus), 60));
        session.setSuccess(success);
        session.setHttpStatusCode(httpStatusCode);
        session.setDurationMillis(Math.max(0L, duration == null ? 0L : duration.toMillis()));
        session.setHardwareBacked(context != null && context.hardwareBacked());
        session.setMutualTls(context != null && context.mutualTls());
        session.setHostnameVerification(context != null && context.binding() != null && context.binding().hostnameVerification());
        session.setKeyStoreRef(context != null && context.binding() != null ? truncate(normalized(context.binding().keyStoreRef()), 160) : null);
        session.setTrustStoreRef(context != null && context.binding() != null ? truncate(normalized(context.binding().trustStoreRef()), 160) : null);
        session.setKeyAlias(context != null ? truncate(normalized(context.selectedKeyAlias()), 255) : null);
        session.setCorrelationId(request != null ? truncate(normalized(request.correlationId()), 200) : null);
        session.setMetadataJson(writeMetadata(context, request, httpStatusCode, failure));
        repository.save(session);
    }

    private String writeMetadata(JudicialConnectorCryptographicContext context,
                                 JudicialSecureHttpRequest request,
                                 Integer httpStatusCode,
                                 Throwable failure) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        if (context != null && context.binding() != null) {
            metadata.put("bindingId", context.binding().bindingId());
            metadata.put("allowedHosts", context.binding().allowedHosts());
            metadata.put("protocols", context.binding().protocols());
            metadata.put("cipherSuites", context.binding().cipherSuites());
        }
        if (request != null) {
            metadata.put("method", normalized(request.method()));
            metadata.put("requestMetadata", request.metadata());
        }
        if (httpStatusCode != null) {
            metadata.put("httpStatusCode", httpStatusCode);
        }
        if (failure != null) {
            metadata.put("failureClass", failure.getClass().getName());
            metadata.put("failureType", classifyFailure(failure));
        }
        try {
            return objectMapper.writeValueAsString(JudicialMapSupport.copyNonNull(metadata));
        } catch (Exception ex) {
            return "{\"serializationError\":true}";
        }
    }

    private JudicialConnectorSecuritySessionReport toReport(JudicialConnectorSecuritySession session) {
        return new JudicialConnectorSecuritySessionReport(
                session.getCreatedAt(),
                session.getConnectorSystem(),
                session.getTribunalCodigo(),
                session.getEnvironmentName(),
                session.getOperationName(),
                session.getTargetScheme(),
                session.getTargetHostSha256(),
                session.getTargetPort(),
                session.getTlsMode(),
                session.getOutcomeStatus(),
                session.isSuccess(),
                session.getHttpStatusCode(),
                session.getDurationMillis(),
                session.isHardwareBacked(),
                session.isMutualTls(),
                session.isHostnameVerification(),
                session.getKeyStoreRef(),
                session.getTrustStoreRef(),
                session.getKeyAlias(),
                session.getCorrelationId(),
                JudicialMapSupport.compact("metadataJson", session.getMetadataJson())
        );
    }

    private String classifyFailure(Throwable failure) {
        if (failure == null) {
            return "UNSPECIFIED_FAILURE";
        }
        if (failure instanceof javax.net.ssl.SSLHandshakeException) {
            return "HANDSHAKE_FAILURE";
        }
        if (failure instanceof javax.net.ssl.SSLException) {
            return "TLS_FAILURE";
        }
        if (failure instanceof java.io.IOException) {
            return "IO_FAILURE";
        }
        if (failure instanceof InterruptedException) {
            return "INTERRUPTED";
        }
        return "TRANSPORT_FAILURE";
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(Objects.requireNonNullElse(value, "").getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new JudicialConnectorCryptographicException("Unable to compute security session fingerprint.", ex);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String normalized(String value) {
        if (value == null) {
            return null;
        }
        String out = value.trim();
        return out.isBlank() ? null : out.toUpperCase(Locale.ROOT);
    }
}
