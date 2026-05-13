package com.tcc.pjb.backend.service.api;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.crypto.CryptoVaultService;
import com.tcc.pjb.backend.model.entity.api.MarketplaceWebhookDelivery;
import com.tcc.pjb.backend.model.entity.api.MarketplaceWebhookEndpoint;
import com.tcc.pjb.backend.model.repository.MarketplaceWebhookDeliveryRepository;
import com.tcc.pjb.backend.model.repository.MarketplaceWebhookEndpointRepository;

@Service
public class MarketplaceWebhookDispatcherService {

    private static final List<String> READY_STATUSES = List.of("PENDENTE", "RETENTATIVA");
    private static final int MAX_ATTEMPTS = 7;

    private final MarketplaceWebhookDeliveryRepository deliveryRepository;
    private final MarketplaceWebhookEndpointRepository endpointRepository;
    private final CryptoVaultService cryptoVaultService;
    private final HttpClient httpClient;

    public MarketplaceWebhookDispatcherService(MarketplaceWebhookDeliveryRepository deliveryRepository,
                                               MarketplaceWebhookEndpointRepository endpointRepository,
                                               CryptoVaultService cryptoVaultService) {
        this.deliveryRepository = Objects.requireNonNull(deliveryRepository);
        this.endpointRepository = Objects.requireNonNull(endpointRepository);
        this.cryptoVaultService = Objects.requireNonNull(cryptoVaultService);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Transactional
    public DispatchBatchResult dispatchPending(int limit) {
        int effectiveLimit = Math.max(1, Math.min(200, limit));
        List<MarketplaceWebhookDelivery> pendentes = deliveryRepository
                .findTop200ByStatusInAndNextRetryAtBeforeOrderByCreatedAtAsc(READY_STATUSES, Instant.now())
                .stream()
                .limit(effectiveLimit)
                .toList();
        int delivered = 0;
        int retriable = 0;
        int dead = 0;
        for (MarketplaceWebhookDelivery delivery : pendentes) {
            DeliveryDispatchResult result = dispatchOne(delivery);
            switch (result.status()) {
                case "ENTREGUE" -> delivered++;
                case "RETENTATIVA" -> retriable++;
                default -> dead++;
            }
        }
        return new DispatchBatchResult(effectiveLimit, pendentes.size(), delivered, retriable, dead, Instant.now());
    }

    @Transactional
    public DeliveryDispatchResult redispatch(String clientId, Long deliveryId) {
        MarketplaceWebhookDelivery delivery = deliveryRepository.findByIdAndEndpoint_ClientApp_ClientIdIgnoreCase(deliveryId, clientId)
                .orElseThrow(() -> new IllegalArgumentException("Entrega de webhook nao encontrada para o cliente informado."));
        delivery.setStatus("RETENTATIVA");
        delivery.setNextRetryAt(Instant.now());
        deliveryRepository.save(delivery);
        return dispatchOne(delivery);
    }

    @Transactional
    public DeliveryDispatchResult dispatchOne(MarketplaceWebhookDelivery delivery) {
        if (delivery == null || delivery.getId() == null) {
            throw new IllegalArgumentException("Entrega de webhook nao informada.");
        }
        MarketplaceWebhookDelivery managed = deliveryRepository.findById(delivery.getId())
                .orElseThrow(() -> new IllegalArgumentException("Entrega de webhook nao encontrada."));
        MarketplaceWebhookEndpoint endpoint = managed.getEndpoint();
        if (endpoint == null) {
            managed.setStatus("FALHA_FINAL");
            managed.setResponseExcerpt("Endpoint ausente na entrega do webhook.");
            managed.setLastDispatchAt(Instant.now());
            deliveryRepository.save(managed);
            return toResult(managed);
        }
        if (endpoint.getStatus() == null || !"ATIVO".equalsIgnoreCase(endpoint.getStatus())) {
            managed.setStatus("FALHA_FINAL");
            managed.setResponseExcerpt("Endpoint desativado para entrega do webhook.");
            managed.setLastDispatchAt(Instant.now());
            deliveryRepository.save(managed);
            return toResult(managed);
        }

        String payload = managed.getPayloadJson();
        if (payload == null || payload.isBlank()) {
            payload = "{}";
        }

        String signingSecret = endpoint.getSigningSecretCiphertext() == null || endpoint.getSigningSecretCiphertext().isBlank()
                ? null
                : cryptoVaultService.lerDadoBlindado(endpoint.getSigningSecretCiphertext());
        if (signingSecret == null || signingSecret.isBlank()) {
            managed.setAttempts(safeAttempts(managed) + 1);
            managed.setStatus(nextFailureStatus(managed.getAttempts()));
            managed.setResponseExcerpt("Segredo de assinatura indisponivel para o endpoint.");
            managed.setLastDispatchAt(Instant.now());
            managed.setNextRetryAt(nextRetryAt(managed.getAttempts()));
            endpoint.setLastFailureAt(Instant.now());
            endpoint.setLastErrorMessage("Segredo de assinatura indisponivel.");
            endpointRepository.save(endpoint);
            deliveryRepository.save(managed);
            return toResult(managed);
        }

        try {
            URI callbackUri = validateCallbackUri(endpoint.getCallbackUrl());
            String signature = hmacSha256Base64(signingSecret, payload);
            HttpRequest request = HttpRequest.newBuilder(callbackUri)
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "PJB-Marketplace-WebhookDispatcher/2026.1")
                    .header("X-PJB-Event-Type", safe(managed.getEventType()))
                    .header("X-PJB-Delivery-Id", String.valueOf(managed.getId()))
                    .header("X-PJB-Payload-Hash", safe(managed.getPayloadHash()))
                    .header("X-PJB-Signature-Alg", "HMAC-SHA256")
                    .header("X-PJB-Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            managed.setAttempts(safeAttempts(managed) + 1);
            managed.setResponseCode(response.statusCode());
            managed.setLastDispatchAt(Instant.now());
            managed.setResponseExcerpt(truncate(response.body()));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                managed.setStatus("ENTREGUE");
                managed.setDeliveredAt(Instant.now());
                managed.setNextRetryAt(null);
                endpoint.setLastSuccessAt(Instant.now());
                endpoint.setLastErrorMessage(null);
            } else {
                managed.setStatus(nextFailureStatus(managed.getAttempts()));
                managed.setNextRetryAt(nextRetryAt(managed.getAttempts()));
                endpoint.setLastFailureAt(Instant.now());
                endpoint.setLastErrorMessage("HTTP " + response.statusCode());
            }
            endpointRepository.save(endpoint);
            deliveryRepository.save(managed);
            return toResult(managed);
        } catch (Exception e) {
            managed.setAttempts(safeAttempts(managed) + 1);
            managed.setStatus(nextFailureStatus(managed.getAttempts()));
            managed.setResponseExcerpt(truncate(e.getMessage()));
            managed.setLastDispatchAt(Instant.now());
            managed.setNextRetryAt(nextRetryAt(managed.getAttempts()));
            endpoint.setLastFailureAt(Instant.now());
            endpoint.setLastErrorMessage(truncate(e.getMessage()));
            endpointRepository.save(endpoint);
            deliveryRepository.save(managed);
            return toResult(managed);
        }
    }

    private DeliveryDispatchResult toResult(MarketplaceWebhookDelivery delivery) {
        return new DeliveryDispatchResult(
                delivery.getId(),
                delivery.getEndpoint() != null ? delivery.getEndpoint().getId() : null,
                delivery.getStatus(),
                delivery.getResponseCode(),
                safeAttempts(delivery),
                delivery.getNextRetryAt(),
                delivery.getDeliveredAt(),
                delivery.getLastDispatchAt(),
                delivery.getResponseExcerpt()
        );
    }

    private int safeAttempts(MarketplaceWebhookDelivery delivery) {
        return delivery.getAttempts() == null ? 0 : Math.max(0, delivery.getAttempts());
    }

    private String nextFailureStatus(int attempts) {
        return attempts >= MAX_ATTEMPTS ? "FALHA_FINAL" : "RETENTATIVA";
    }

    private Instant nextRetryAt(int attempts) {
        if (attempts >= MAX_ATTEMPTS) {
            return null;
        }
        long backoffMinutes = switch (Math.max(1, attempts)) {
            case 1 -> 1L;
            case 2 -> 5L;
            case 3 -> 15L;
            case 4 -> 30L;
            case 5 -> 60L;
            default -> 180L;
        };
        return Instant.now().plusSeconds(backoffMinutes * 60L);
    }

    private String safe(String value) {
        return value == null ? "" : value;
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

    private URI validateCallbackUri(String callbackUrl) {
        if (callbackUrl == null || callbackUrl.isBlank()) {
            throw new IllegalArgumentException("Webhook callback não informado.");
        }
        URI uri = URI.create(callbackUrl.trim()).normalize();
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().trim().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().trim().toLowerCase(Locale.ROOT);
        boolean local = "localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host);
        if (!("https".equals(scheme) || ("http".equals(scheme) && local))) {
            throw new IllegalArgumentException("Webhook callback deve utilizar HTTPS, exceto em ambiente local controlado.");
        }
        if (host.isBlank() || uri.getUserInfo() != null || uri.getFragment() != null || isUnsafeRemoteHost(host, local)) {
            throw new IllegalArgumentException("Webhook callback aponta para destino não permitido.");
        }
        return uri;
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

    private String hmacSha256Base64(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return java.util.Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao assinar payload de webhook.", e);
        }
    }

    public record DispatchBatchResult(
            int requestedLimit,
            int selectedDeliveries,
            int delivered,
            int retriable,
            int deadLetters,
            Instant processedAt
    ) {
    }

    public record DeliveryDispatchResult(
            Long deliveryId,
            Long endpointId,
            String status,
            Integer responseCode,
            int attempts,
            Instant nextRetryAt,
            Instant deliveredAt,
            Instant lastDispatchAt,
            String responseExcerpt
    ) {
    }
}
