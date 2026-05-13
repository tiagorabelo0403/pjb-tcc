package com.tcc.pjb.backend.core.audit.cross.persistence.repo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.core.audit.cross.persistence.entity.AuditCorrelationIndexEntity;

public interface AuditCorrelationIndexRepository extends JpaRepository<AuditCorrelationIndexEntity, Long> {

    List<AuditCorrelationIndexEntity> findTop50ByCorrelationKeyOrderByCreatedAtDesc(String correlationKey);

    List<AuditCorrelationIndexEntity> findByCorrelationKey(String correlationKey);

    List<AuditCorrelationIndexEntity> findTop50ByResourceTypeAndResourceIdOrderByCreatedAtDesc(String resourceType, String resourceId);
}
