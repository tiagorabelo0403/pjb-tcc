package com.tcc.pjb.backend.core.audit.ledger;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLedgerRepository extends JpaRepository<AuditLedgerEntry, Long> {

    Optional<AuditLedgerEntry> findTopByOrderByIdDesc();

    @Query("""
           select e from AuditLedgerEntry e
           where (:actorUserId is null or e.actorUserId = :actorUserId)
             and (:actionPrefix is null or e.action like concat(:actionPrefix, '%'))
             and (:resourceType is null or e.resourceType = :resourceType)
             and (:resourceId is null or e.resourceId = :resourceId)
           order by e.id desc
           """)
    Page<AuditLedgerEntry> search(@Param("actorUserId") Long actorUserId,
                                 @Param("actionPrefix") String actionPrefix,
                                 @Param("resourceType") String resourceType,
                                 @Param("resourceId") String resourceId,
                                 Pageable pageable);

}
