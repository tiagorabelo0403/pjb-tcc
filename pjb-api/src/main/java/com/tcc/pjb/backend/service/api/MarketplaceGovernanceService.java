package com.tcc.pjb.backend.service.api;

import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.crypto.CryptoVaultService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.api.MarketplaceClientApp;
import com.tcc.pjb.backend.model.entity.api.MarketplaceClientSubscription;
import com.tcc.pjb.backend.model.entity.api.MarketplaceIntegrationPlan;
import com.tcc.pjb.backend.model.entity.api.MarketplaceWebhookDelivery;
import com.tcc.pjb.backend.model.entity.api.MarketplaceWebhookEndpoint;
import com.tcc.pjb.backend.model.repository.MarketplaceClientAppRepository;
import com.tcc.pjb.backend.model.repository.MarketplaceClientSubscriptionRepository;
import com.tcc.pjb.backend.model.repository.MarketplaceIntegrationPlanRepository;
import com.tcc.pjb.backend.model.repository.MarketplaceWebhookDeliveryRepository;
import com.tcc.pjb.backend.model.repository.MarketplaceWebhookEndpointRepository;

@Service
public class MarketplaceGovernanceService {

    private final MarketplaceClientAppRepository clientRepository;
    private final MarketplaceIntegrationPlanRepository planRepository;
    private final MarketplaceClientSubscriptionRepository subscriptionRepository;
    private final MarketplaceWebhookEndpointRepository webhookEndpointRepository;
    private final MarketplaceWebhookDeliveryRepository deliveryRepository;
    private final ObjectMapper objectMapper;
    private final CryptoVaultService cryptoVaultService;

    public MarketplaceGovernanceService(MarketplaceClientAppRepository clientRepository,
                                        MarketplaceIntegrationPlanRepository planRepository,
                                        MarketplaceClientSubscriptionRepository subscriptionRepository,
                                        MarketplaceWebhookEndpointRepository webhookEndpointRepository,
                                        MarketplaceWebhookDeliveryRepository deliveryRepository,
                                        ObjectMapper objectMapper,
                                        CryptoVaultService cryptoVaultService) {
        this.clientRepository = Objects.requireNonNull(clientRepository);
        this.planRepository = Objects.requireNonNull(planRepository);
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
        this.webhookEndpointRepository = Objects.requireNonNull(webhookEndpointRepository);
        this.deliveryRepository = Objects.requireNonNull(deliveryRepository);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.cryptoVaultService = Objects.requireNonNull(cryptoVaultService);
    }

    @Transactional(readOnly = true)
    public List<PlanView> listarPlanos() {
        return planRepository.findAll().stream()
                .map(plan -> new PlanView(
                        plan.getId(),
                        plan.getCode(),
                        plan.getDisplayName(),
                        plan.getStatus(),
                        plan.getMaxProtocolosDia(),
                        plan.getMaxWebhookEndpoints(),
                        plan.isAllowStreaming(),
                        plan.isAllowHighVolume(),
                        plan.getDescription(),
                        plan.getCreatedAt()))
                .toList();
    }

    @Transactional
    public PlanView criarPlano(PlanRequest request) {
        MarketplaceIntegrationPlan plan = planRepository.findByCodeIgnoreCase(request.code()).orElseGet(MarketplaceIntegrationPlan::new);
        plan.setCode(normalizeUpper(request.code()));
        plan.setDisplayName(request.displayName());
        plan.setStatus("ATIVO");
        plan.setMaxProtocolosDia(Math.max(1, request.maxProtocolosDia()));
        plan.setMaxWebhookEndpoints(Math.max(1, request.maxWebhookEndpoints()));
        plan.setAllowStreaming(request.allowStreaming());
        plan.setAllowHighVolume(request.allowHighVolume());
        plan.setDescription(request.description());
        MarketplaceIntegrationPlan saved = planRepository.save(plan);
        return new PlanView(saved.getId(), saved.getCode(), saved.getDisplayName(), saved.getStatus(), saved.getMaxProtocolosDia(),
                saved.getMaxWebhookEndpoints(), saved.isAllowStreaming(), saved.isAllowHighVolume(), saved.getDescription(), saved.getCreatedAt());
    }

    @Transactional
    public SubscriptionView vincularCliente(String clientId, SubscriptionRequest request) {
        MarketplaceClientApp client = clientRepository.findByClientIdIgnoreCase(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente do marketplace nao encontrado."));
        MarketplaceIntegrationPlan plan = planRepository.findByCodeIgnoreCase(request.planCode())
                .orElseThrow(() -> new IllegalArgumentException("Plano do marketplace nao encontrado."));
        MarketplaceClientSubscription subscription = subscriptionRepository.findFirstByClientApp_ClientIdIgnoreCaseAndStatusOrderByStartedAtDesc(clientId, "ATIVA")
                .orElseGet(MarketplaceClientSubscription::new);
        subscription.setClientApp(client);
        subscription.setPlan(plan);
        subscription.setStatus("ATIVA");
        subscription.setStartedAt(request.startedAt() != null ? request.startedAt() : Instant.now());
        subscription.setEndsAt(request.endsAt());
        subscription.setWebhookEndpointLimitOverride(request.webhookEndpointLimitOverride());
        subscription.setObservacoes(request.observacoes());
        MarketplaceClientSubscription saved = subscriptionRepository.save(subscription);
        return toSubscriptionView(saved);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionView> listarAssinaturas(String clientId) {
        return subscriptionRepository.findByClientApp_ClientIdIgnoreCaseOrderByStartedAtDesc(clientId).stream().map(this::toSubscriptionView).toList();
    }

    @Transactional
    public WebhookEndpointView registrarWebhook(String clientId, WebhookRequest request) {
        MarketplaceClientApp client = clientRepository.findByClientIdIgnoreCase(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente do marketplace nao encontrado."));
        MarketplaceClientSubscription active = requireSubscription(clientId);
        long activeEndpoints = webhookEndpointRepository.countByClientApp_ClientIdIgnoreCaseAndStatusIgnoreCase(clientId, "ATIVO");
        int limit = active.getWebhookEndpointLimitOverride() != null && active.getWebhookEndpointLimitOverride() > 0
                ? active.getWebhookEndpointLimitOverride()
                : active.getPlan().getMaxWebhookEndpoints();
        if (activeEndpoints >= limit) {
            throw new IllegalStateException("Limite de endpoints webhook do plano foi atingido.");
        }
        String callbackUrl = normalizeCallbackUrl(request.callbackUrl());
        MarketplaceWebhookEndpoint endpoint = new MarketplaceWebhookEndpoint();
        endpoint.setClientApp(client);
        endpoint.setSubscription(active);
        endpoint.setCallbackUrl(callbackUrl);
        endpoint.setEventFilter(normalizeUpper(request.eventFilter()));
        endpoint.setSigningSecretHash(Hashes.sha256Hex(request.signingSecret()));
        endpoint.setSigningSecretCiphertext(cryptoVaultService.blindarDado(request.signingSecret()));
        endpoint.setStatus("ATIVO");
        MarketplaceWebhookEndpoint saved = webhookEndpointRepository.save(endpoint);
        return toWebhookView(saved);
    }

    @Transactional(readOnly = true)
    public List<WebhookEndpointView> listarWebhooks(String clientId) {
        return webhookEndpointRepository.findByClientApp_ClientIdIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(clientId, "ATIVO")
                .stream().map(this::toWebhookView).toList();
    }

    @Transactional(readOnly = true)
    public void assertClientCanProtocol(String clientId) {
        MarketplaceClientSubscription subscription = requireSubscription(clientId);
        Instant now = Instant.now();
        if (subscription.getEndsAt() != null && subscription.getEndsAt().isBefore(now)) {
            throw new IllegalStateException("Assinatura do cliente esta expirada.");
        }
        if (subscription.getUltimoResetContador() == null || subscription.getUltimoResetContador().plus(1, ChronoUnit.DAYS).isBefore(now)) {
            return;
        }
        int current = subscription.getProtocolosDiaAtual() == null ? 0 : subscription.getProtocolosDiaAtual();
        int limit = subscription.getPlan().getMaxProtocolosDia() == null ? 0 : subscription.getPlan().getMaxProtocolosDia();
        if (limit > 0 && current >= limit) {
            throw new IllegalStateException("Limite diario de protocolos do plano foi atingido.");
        }
    }

    @Transactional
    public void registrarConsumoProtocolo(String clientId) {
        MarketplaceClientSubscription subscription = requireSubscription(clientId);
        Instant now = Instant.now();
        if (subscription.getUltimoResetContador() == null || subscription.getUltimoResetContador().plus(1, ChronoUnit.DAYS).isBefore(now)) {
            subscription.setProtocolosDiaAtual(0);
            subscription.setUltimoResetContador(now);
        }
        subscription.setProtocolosDiaAtual((subscription.getProtocolosDiaAtual() == null ? 0 : subscription.getProtocolosDiaAtual()) + 1);
        subscriptionRepository.save(subscription);
    }

    @Transactional
    public List<WebhookDeliveryView> publicarEventoProtocolo(String clientId, Long processoId, String numeroProcesso, String protocoloMarketplace) {
        requireSubscription(clientId);
        List<MarketplaceWebhookEndpoint> endpoints = webhookEndpointRepository.findByClientApp_ClientIdIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(clientId, "ATIVO");
        if (endpoints.isEmpty()) {
            return List.of();
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", "PROCESSO_PROTOCOLADO");
        payload.put("clientId", clientId);
        payload.put("processoId", processoId);
        payload.put("numeroProcesso", numeroProcesso);
        payload.put("protocoloMarketplace", protocoloMarketplace);
        payload.put("occurredAt", Instant.now().toString());
        String serialized = writeJson(payload);
        String payloadHash = Hashes.sha256HexBytes(serialized.getBytes(StandardCharsets.UTF_8));
        return endpoints.stream()
                .filter(endpoint -> acceptsEvent(endpoint.getEventFilter(), "PROCESSO_PROTOCOLADO"))
                .map(endpoint -> {
                    MarketplaceWebhookDelivery delivery = new MarketplaceWebhookDelivery();
                    delivery.setEndpoint(endpoint);
                    delivery.setEventType("PROCESSO_PROTOCOLADO");
                    delivery.setPayloadHash(payloadHash);
                    delivery.setStatus("PENDENTE");
                    delivery.setPayloadJson(serialized);
                    delivery.setResponseCode(null);
                    delivery.setAttempts(0);
                    delivery.setNextRetryAt(Instant.now());
                    delivery.setResponseExcerpt("Queued for outbound delivery by marketplace dispatcher.");
                    MarketplaceWebhookDelivery saved = deliveryRepository.save(delivery);
                    return new WebhookDeliveryView(saved.getId(), endpoint.getId(), endpoint.getCallbackUrl(), saved.getEventType(),
                            saved.getStatus(), saved.getResponseCode(), saved.getAttempts(), saved.getCreatedAt(), saved.getNextRetryAt(), saved.getDeliveredAt());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WebhookDeliveryView> listarEntregas(String clientId) {
        return deliveryRepository.findTop100ByEndpoint_ClientApp_ClientIdIgnoreCaseOrderByCreatedAtDesc(clientId)
                .stream()
                .map(delivery -> new WebhookDeliveryView(
                        delivery.getId(),
                        delivery.getEndpoint() != null ? delivery.getEndpoint().getId() : null,
                        delivery.getEndpoint() != null ? delivery.getEndpoint().getCallbackUrl() : null,
                        delivery.getEventType(),
                        delivery.getStatus(),
                        delivery.getResponseCode(),
                        delivery.getAttempts(),
                        delivery.getCreatedAt(),
                        delivery.getNextRetryAt(),
                        delivery.getDeliveredAt()))
                .toList();
    }

    private MarketplaceClientSubscription requireSubscription(String clientId) {
        return subscriptionRepository.findFirstByClientApp_ClientIdIgnoreCaseAndStatusOrderByStartedAtDesc(clientId, "ATIVA")
                .orElseThrow(() -> new IllegalStateException("Cliente sem assinatura ativa de marketplace."));
    }

    private SubscriptionView toSubscriptionView(MarketplaceClientSubscription subscription) {
        return new SubscriptionView(
                subscription.getId(),
                subscription.getClientApp() != null ? subscription.getClientApp().getClientId() : null,
                subscription.getPlan() != null ? subscription.getPlan().getCode() : null,
                subscription.getPlan() != null ? subscription.getPlan().getDisplayName() : null,
                subscription.getStatus(),
                subscription.getStartedAt(),
                subscription.getEndsAt(),
                subscription.getProtocolosDiaAtual(),
                subscription.getWebhookEndpointLimitOverride(),
                subscription.getObservacoes()
        );
    }

    private WebhookEndpointView toWebhookView(MarketplaceWebhookEndpoint endpoint) {
        return new WebhookEndpointView(
                endpoint.getId(),
                endpoint.getClientApp() != null ? endpoint.getClientApp().getClientId() : null,
                endpoint.getCallbackUrl(),
                endpoint.getEventFilter(),
                endpoint.getStatus(),
                endpoint.getLastSuccessAt(),
                endpoint.getLastFailureAt(),
                endpoint.getCreatedAt()
        );
    }

    private String normalizeCallbackUrl(String callbackUrl) {
        if (callbackUrl == null || callbackUrl.isBlank()) {
            throw new IllegalArgumentException("Callback URL do webhook não informada.");
        }
        URI uri = URI.create(callbackUrl.trim()).normalize();
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().trim().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().trim().toLowerCase(Locale.ROOT);
        boolean local = "localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host);
        if (!("https".equals(scheme) || ("http".equals(scheme) && local))) {
            throw new IllegalArgumentException("Callback URL deve utilizar HTTPS, exceto em ambiente local controlado.");
        }
        if (host.isBlank() || uri.getUserInfo() != null || uri.getFragment() != null || isUnsafeRemoteHost(host, local)) {
            throw new IllegalArgumentException("Callback URL do webhook aponta para destino não permitido.");
        }
        return uri.toString();
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

    private boolean acceptsEvent(String eventFilter, String eventType) {
        if (eventFilter == null || eventFilter.isBlank()) {
            return true;
        }
        if ("*".equals(eventFilter.trim())) {
            return true;
        }
        return normalizeUpper(eventFilter).contains(normalizeUpper(eventType));
    }

    private String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar payload do webhook do marketplace.", e);
        }
    }

    private String normalizeUpper(String input) {
        return input == null ? "" : input.trim().toUpperCase(java.util.Locale.ROOT);
    }

    public record PlanRequest(
            @NotBlank String code,
            @NotBlank String displayName,
            @Min(1) int maxProtocolosDia,
            @Min(1) int maxWebhookEndpoints,
            boolean allowStreaming,
            boolean allowHighVolume,
            String description
    ) {
    }

    public record SubscriptionRequest(
            @NotBlank String planCode,
            Instant startedAt,
            Instant endsAt,
            Integer webhookEndpointLimitOverride,
            String observacoes
    ) {
    }

    public record WebhookRequest(
            @NotBlank String callbackUrl,
            @NotBlank String eventFilter,
            @NotBlank String signingSecret
    ) {
    }

    public record PlanView(
            Long id,
            String code,
            String displayName,
            String status,
            Integer maxProtocolosDia,
            Integer maxWebhookEndpoints,
            boolean allowStreaming,
            boolean allowHighVolume,
            String description,
            Instant createdAt
    ) {
    }

    public record SubscriptionView(
            Long id,
            String clientId,
            String planCode,
            String planName,
            String status,
            Instant startedAt,
            Instant endsAt,
            Integer protocolosDiaAtual,
            Integer webhookEndpointLimitOverride,
            String observacoes
    ) {
    }

    public record WebhookEndpointView(
            Long id,
            String clientId,
            String callbackUrl,
            String eventFilter,
            String status,
            Instant lastSuccessAt,
            Instant lastFailureAt,
            Instant createdAt
    ) {
    }

    public record WebhookDeliveryView(
            Long id,
            Long endpointId,
            String callbackUrl,
            String eventType,
            String status,
            Integer responseCode,
            Integer attempts,
            Instant createdAt,
            Instant nextRetryAt,
            Instant deliveredAt
    ) {
    }
}
