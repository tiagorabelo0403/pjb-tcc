package com.tcc.pjb.backend.model.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.api.MarketplaceAccessTokenRecord;

@Repository
public interface MarketplaceAccessTokenRecordRepository extends JpaRepository<MarketplaceAccessTokenRecord, Long> {

    Optional<MarketplaceAccessTokenRecord> findByJti(String jti);

    Optional<MarketplaceAccessTokenRecord> findByTokenHash(String tokenHash);

    List<MarketplaceAccessTokenRecord> findTop100ByClientApp_IdOrderByIssuedAtDesc(Long clientAppId);

    long deleteByStatusAndExpiresAtBefore(String status, Instant expiresAt);
}
