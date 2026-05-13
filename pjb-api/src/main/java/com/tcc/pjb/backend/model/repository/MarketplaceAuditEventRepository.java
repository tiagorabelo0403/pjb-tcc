package com.tcc.pjb.backend.model.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.api.MarketplaceAuditEvent;

@Repository
public interface MarketplaceAuditEventRepository extends JpaRepository<MarketplaceAuditEvent, Long> {

    List<MarketplaceAuditEvent> findTop100ByClientApp_IdOrderByCreatedAtDesc(Long clientAppId);
}
