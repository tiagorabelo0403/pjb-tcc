package com.tcc.pjb.backend.model.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.api.MarketplaceWebhookDelivery;

@Repository
public interface MarketplaceWebhookDeliveryRepository extends JpaRepository<MarketplaceWebhookDelivery, Long> {

    List<MarketplaceWebhookDelivery> findTop100ByEndpoint_ClientApp_ClientIdIgnoreCaseOrderByCreatedAtDesc(String clientId);

    List<MarketplaceWebhookDelivery> findTop200ByStatusInAndNextRetryAtBeforeOrderByCreatedAtAsc(Collection<String> statuses, Instant referenceTime);

    Optional<MarketplaceWebhookDelivery> findByIdAndEndpoint_ClientApp_ClientIdIgnoreCase(Long id, String clientId);
}
