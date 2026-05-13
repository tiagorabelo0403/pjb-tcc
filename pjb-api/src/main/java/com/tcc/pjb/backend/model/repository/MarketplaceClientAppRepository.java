package com.tcc.pjb.backend.model.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.api.MarketplaceClientApp;

@Repository
public interface MarketplaceClientAppRepository extends JpaRepository<MarketplaceClientApp, Long> {

    Optional<MarketplaceClientApp> findByClientIdIgnoreCase(String clientId);

    boolean existsByClientIdIgnoreCase(String clientId);
}
