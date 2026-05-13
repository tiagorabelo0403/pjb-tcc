package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.api.MarketplaceClientSubscription;

@Repository
public interface MarketplaceClientSubscriptionRepository extends JpaRepository<MarketplaceClientSubscription, Long> {

    Optional<MarketplaceClientSubscription> findFirstByClientApp_ClientIdIgnoreCaseAndStatusOrderByStartedAtDesc(String clientId, String status);

    List<MarketplaceClientSubscription> findByClientApp_ClientIdIgnoreCaseOrderByStartedAtDesc(String clientId);
}
