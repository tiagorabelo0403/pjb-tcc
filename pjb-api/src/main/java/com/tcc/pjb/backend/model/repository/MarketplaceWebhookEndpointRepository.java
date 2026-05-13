package com.tcc.pjb.backend.model.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.api.MarketplaceWebhookEndpoint;

@Repository
public interface MarketplaceWebhookEndpointRepository extends JpaRepository<MarketplaceWebhookEndpoint, Long> {

    List<MarketplaceWebhookEndpoint> findByClientApp_ClientIdIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(String clientId, String status);

    long countByClientApp_ClientIdIgnoreCaseAndStatusIgnoreCase(String clientId, String status);
}
