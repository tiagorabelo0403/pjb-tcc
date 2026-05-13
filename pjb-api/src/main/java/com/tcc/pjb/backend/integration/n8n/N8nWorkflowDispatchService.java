package com.tcc.pjb.backend.integration.n8n;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.integration.n8n.N8nDispatchRequest;
import com.tcc.pjb.backend.model.dto.integration.n8n.N8nDispatchResponse;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class N8nWorkflowDispatchService {

    private final N8nIntegrationProperties properties;
    private final N8nSignatureService signatureService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ObjectProvider<AuditLedgerService> auditLedgerServiceProvider;

    public N8nWorkflowDispatchService(N8nIntegrationProperties properties,
                                      N8nSignatureService signatureService,
                                      ObjectMapper objectMapper,
                                      @Qualifier("pjbSharedHttpClient") HttpClient httpClient,
                                      ObjectProvider<AuditLedgerService> auditLedgerServiceProvider) {
        this.properties = Objects.requireNonNull(properties);
        this.signatureService = Objects.requireNonNull(signatureService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.httpClient = Objects.requireNonNull(httpClient);
        this.auditLedgerServiceProvider = Objects.requireNonNull(auditLedgerServiceProvider);
    }

    public N8nDispatchResponse dispatch(N8nDispatchRequest request) {
        Objects.requireNonNull(request, "request");
        Instant now = Instant.now();
        String traceId = normalizeToken(request.traceId(), request.requestId());
        if (!properties.isEnabled()) {
            return new N8nDispatchResponse(false, 503, request.requestId(), traceId, null, now, null, "Integração n8n desabilitada.");
        }
        URI endpoint = resolveEndpoint();
        String payload = writePayload(request, traceId, now);
        if (payload.getBytes(StandardCharsets.UTF_8).length > properties.getMaxPayloadBytes()) {
            throw new IllegalArgumentException("Payload n8n excede o limite configurado.");
        }
        String payloadHash = Hashes.sha256Hex(payload);
        String signature = signatureService.sign(properties.getDispatchSecret(), payload);
        try {
            HttpRequest outbound = HttpRequest.newBuilder(endpoint)
                    .timeout(properties.getRequestTimeout())
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "PJB-N8N-Dispatch/2026.1")
                    .header("X-PJB-Event-Type", normalizeToken(request.eventType(), "PJB_EVENT"))
                    .header("X-PJB-Request-Id", normalizeToken(request.requestId(), traceId))
                    .header("X-PJB-Trace-Id", traceId)
                    .header("X-PJB-Tenant", normalizeToken(firstNonBlank(request.tenant(), properties.getTenant()), "pjb"))
                    .header("X-PJB-Payload-Hash", payloadHash)
                    .header("X-PJB-Signature-Alg", "HMAC-SHA256")
                    .header("X-PJB-Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(outbound, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            N8nDispatchResponse result = new N8nDispatchResponse(
                    response.statusCode() >= 200 && response.statusCode() < 300,
                    response.statusCode(),
                    request.requestId(),
                    traceId,
                    endpoint.toString(),
                    now,
                    payloadHash,
                    truncate(response.body())
            );
            appendAudit("N8N_OUTBOUND_DISPATCH", request.requestId(), payloadHash, "status=" + response.statusCode() + " workflow=" + request.workflowKey());
            if (!result.accepted() && properties.isFailOnDispatchError()) {
                throw new IllegalStateException("n8n rejeitou o evento com status HTTP " + response.statusCode());
            }
            return result;
        } catch (RuntimeException e) {
            appendAudit("N8N_OUTBOUND_DISPATCH_ERROR", request.requestId(), payloadHash, truncate(e.getMessage()));
            throw e;
        } catch (Exception e) {
            appendAudit("N8N_OUTBOUND_DISPATCH_ERROR", request.requestId(), payloadHash, truncate(e.getMessage()));
            if (properties.isFailOnDispatchError()) {
                throw new IllegalStateException("Falha ao despachar evento para n8n.", e);
            }
            return new N8nDispatchResponse(false, 599, request.requestId(), traceId, endpoint.toString(), now, payloadHash, truncate(e.getMessage()));
        }
    }

    private String writePayload(N8nDispatchRequest request, String traceId, Instant now) {
        try {
            LinkedHashMap<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("source", "PJB");
            envelope.put("eventType", normalizeToken(request.eventType(), "PJB_EVENT"));
            envelope.put("workflowKey", normalizeToken(request.workflowKey(), "WORKFLOW"));
            envelope.put("requestId", normalizeToken(request.requestId(), traceId));
            envelope.put("traceId", traceId);
            envelope.put("tenant", normalizeToken(firstNonBlank(request.tenant(), properties.getTenant()), "pjb"));
            envelope.put("occurredAt", now);
            envelope.put("payload", request.payload());
            envelope.put("headers", request.headers());
            return objectMapper.writeValueAsString(envelope);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar envelope do n8n.", e);
        }
    }

    private URI resolveEndpoint() {
        String baseUrl = firstNonBlank(properties.getBaseUrl(), null);
        if (baseUrl == null) {
            throw new IllegalStateException("Base URL do n8n não configurada.");
        }
        URI base = URI.create(baseUrl.trim()).normalize();
        URI endpoint = base.resolve(firstNonBlank(properties.getDispatchPath(), "/webhook/pjb-event-bus"));
        validateRemoteEndpoint(endpoint);
        return endpoint;
    }

    private void validateRemoteEndpoint(URI endpoint) {
        String scheme = endpoint.getScheme() == null ? "" : endpoint.getScheme().trim().toLowerCase(Locale.ROOT);
        String host = endpoint.getHost() == null ? "" : endpoint.getHost().trim().toLowerCase(Locale.ROOT);
        boolean local = "localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host);
        if (properties.isRequireHttps() && !("https".equals(scheme) || properties.isAllowLocalHttp() && "http".equals(scheme) && local)) {
            throw new IllegalArgumentException("Endpoint do n8n deve utilizar HTTPS, exceto em ambiente local controlado.");
        }
        if (host.isBlank() || endpoint.getUserInfo() != null || endpoint.getFragment() != null || isUnsafeRemoteHost(host, local)) {
            throw new IllegalArgumentException("Endpoint do n8n aponta para destino não permitido.");
        }
    }

    private boolean isUnsafeRemoteHost(String host, boolean local) {
        if (local) {
            return false;
        }
        String normalized = host == null ? "" : host.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || normalized.endsWith(".local") || normalized.endsWith(".internal") || "0.0.0.0".equals(normalized)) {
            return true;
        }
        try {
            InetAddress address = InetAddress.getByName(normalized);
            return address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()
                    || address.isMulticastAddress();
        } catch (Exception ignored) {
            return false;
        }
    }

    private void appendAudit(String eventCode, String requestId, String payloadHash, String description) {
        AuditLedgerService ledger = auditLedgerServiceProvider.getIfAvailable();
        if (ledger != null) {
            ledger.appendSafely(eventCode, "N8N", requestId, payloadHash, description == null ? "" : description);
        }
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 500) {
            return normalized;
        }
        return normalized.substring(0, 500);
    }

    private String normalizeToken(String value, String fallback) {
        String normalized = firstNonBlank(value, fallback);
        return normalized == null ? null : normalized.trim();
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback != null && !fallback.isBlank() ? fallback : null;
    }
}
