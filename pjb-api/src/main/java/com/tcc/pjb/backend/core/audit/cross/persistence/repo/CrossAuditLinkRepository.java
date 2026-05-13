package com.tcc.pjb.backend.core.audit.cross.persistence.repo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.core.audit.cross.persistence.entity.CrossAuditLinkEntity;

public interface CrossAuditLinkRepository extends JpaRepository<CrossAuditLinkEntity, Long> {

    List<CrossAuditLinkEntity> findTop50ByCorrelationKeyOrderByCreatedAtDesc(String correlationKey);

    List<CrossAuditLinkEntity> findTop50ByLeftResourceTypeAndLeftResourceIdOrderByCreatedAtDesc(String leftResourceType, String leftResourceId);

    List<CrossAuditLinkEntity> findTop50ByRightResourceTypeAndRightResourceIdOrderByCreatedAtDesc(String rightResourceType, String rightResourceId);
}
