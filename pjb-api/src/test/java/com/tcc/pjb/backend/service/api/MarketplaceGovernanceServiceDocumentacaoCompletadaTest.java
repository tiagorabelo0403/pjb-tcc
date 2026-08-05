package com.tcc.pjb.backend.service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.crypto.CryptoVaultService;
import com.tcc.pjb.backend.model.entity.api.MarketplaceClientApp;
import com.tcc.pjb.backend.model.entity.api.MarketplaceClientSubscription;
import com.tcc.pjb.backend.model.entity.api.MarketplaceIntegrationPlan;
import com.tcc.pjb.backend.model.entity.api.MarketplaceWebhookEndpoint;
import com.tcc.pjb.backend.model.repository.MarketplaceClientAppRepository;
import com.tcc.pjb.backend.model.repository.MarketplaceClientSubscriptionRepository;
import com.tcc.pjb.backend.model.repository.MarketplaceIntegrationPlanRepository;
import com.tcc.pjb.backend.model.repository.MarketplaceWebhookDeliveryRepository;
import com.tcc.pjb.backend.model.repository.MarketplaceWebhookEndpointRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarketplaceGovernanceServiceDocumentacaoCompletadaTest {

    private MarketplaceClientSubscriptionRepository subscriptionRepository;
    private MarketplaceWebhookEndpointRepository webhookEndpointRepository;
    private MarketplaceWebhookDeliveryRepository deliveryRepository;
    private MarketplaceGovernanceService service;

    @BeforeEach
    void setUp() {
        subscriptionRepository = mock(MarketplaceClientSubscriptionRepository.class);
        webhookEndpointRepository = mock(MarketplaceWebhookEndpointRepository.class);
        deliveryRepository = mock(MarketplaceWebhookDeliveryRepository.class);
        MarketplaceIntegrationPlan plan = new MarketplaceIntegrationPlan();
        MarketplaceClientSubscription subscription = new MarketplaceClientSubscription();
        subscription.setPlan(plan);
        when(subscriptionRepository.findFirstByClientApp_ClientIdIgnoreCaseAndStatusOrderByStartedAtDesc("client-teste", "ATIVA"))
                .thenReturn(java.util.Optional.of(subscription));
        MarketplaceClientApp client = new MarketplaceClientApp();
        MarketplaceWebhookEndpoint endpoint = new MarketplaceWebhookEndpoint();
        endpoint.setClientApp(client);
        endpoint.setCallbackUrl("https://integrador.exemplo/webhook");
        endpoint.setEventFilter("*");
        endpoint.setStatus("ATIVO");
        when(webhookEndpointRepository.findByClientApp_ClientIdIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc("client-teste", "ATIVO"))
                .thenReturn(List.of(endpoint));
        when(deliveryRepository.save(any())).thenAnswer(inv -> {
            var d = (com.tcc.pjb.backend.model.entity.api.MarketplaceWebhookDelivery) inv.getArgument(0);
            d.setId(1L);
            d.setCreatedAt(Instant.now());
            return d;
        });
        service = new MarketplaceGovernanceService(
                mock(MarketplaceClientAppRepository.class),
                mock(MarketplaceIntegrationPlanRepository.class),
                subscriptionRepository,
                webhookEndpointRepository,
                deliveryRepository,
                new ObjectMapper(),
                mock(CryptoVaultService.class));
    }

    @Test
    void publicaEventoDocumentacaoCompletadaParaEndpointsAtivos() {
        var entregas = service.publicarEventoDocumentacaoCompletada("client-teste", 1L, "0001-1.2026", "client-teste:ref");

        assertThat(entregas).hasSize(1);
        assertThat(entregas.get(0).eventType()).isEqualTo("PROCESSO_DOCUMENTACAO_COMPLETADA");
    }
}
