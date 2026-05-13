package com.tcc.pjb.backend.core.security.abac;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PjbAuthorizationTrailReadModelRepository extends JpaRepository<PjbAuthorizationTrailReadModelEntry, Long> {

    boolean existsByPayloadHash(String payloadHash);


    PjbAuthorizationTrailReadModelEntry findFirstByOrderByOccurredAtAscIdAsc();

    PjbAuthorizationTrailReadModelEntry findFirstByOrderByOccurredAtDescIdDesc();

    long countByOccurredAtBefore(LocalDateTime cutoff);

    List<PjbAuthorizationTrailReadModelEntry> findByOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAscIdAsc(LocalDateTime occurredAfter,
                                                                                                                             LocalDateTime occurredBefore);

    @Query("""
           select e from PjbAuthorizationTrailReadModelEntry e
           where (:actionPrefix is null or e.action like concat(:actionPrefix, '%'))
             and (:resourceType is null or e.resourceType = :resourceType)
             and (:resourceId is null or e.resourceId = :resourceId)
             and (:actorId is null or e.actorId = :actorId)
             and (:actorType is null or e.actorType = :actorType)
             and (:allowed is null or e.allowed = :allowed)
             and (:riskLevel is null or e.riskLevel = :riskLevel)
             and (:requestId is null or e.requestId = :requestId)
             and (:integrationCode is null or e.integrationCode = :integrationCode)
             and (:institutionalUnitCode is null or e.institutionalUnitCode = :institutionalUnitCode)
             and (:institutionalBoxCode is null or e.institutionalBoxCode = :institutionalBoxCode)
             and (:institutionalCapabilityCode is null or e.institutionalCapabilityCode = :institutionalCapabilityCode)
             and (:governanceChannel is null or e.governanceChannel = :governanceChannel)
             and (:governanceScope is null or e.governanceScope = :governanceScope)
             and (:governanceRequired is null or e.governanceRequired = :governanceRequired)
             and (:governanceSatisfied is null or e.governanceSatisfied = :governanceSatisfied)
             and (:stepUpRequired is null or e.stepUpRequired = :stepUpRequired)
             and (:stepUpSatisfied is null or e.stepUpSatisfied = :stepUpSatisfied)
             and (:occurredAfter is null or e.occurredAt >= :occurredAfter)
             and (:occurredBefore is null or e.occurredAt <= :occurredBefore)
           order by e.occurredAt desc, e.id desc
           """)
    Page<PjbAuthorizationTrailReadModelEntry> search(@Param("actionPrefix") String actionPrefix,
                                                     @Param("resourceType") String resourceType,
                                                     @Param("resourceId") String resourceId,
                                                     @Param("actorId") Long actorId,
                                                     @Param("actorType") String actorType,
                                                     @Param("allowed") Boolean allowed,
                                                     @Param("riskLevel") String riskLevel,
                                                     @Param("requestId") String requestId,
                                                     @Param("integrationCode") String integrationCode,
                                                     @Param("institutionalUnitCode") String institutionalUnitCode,
                                                     @Param("institutionalBoxCode") String institutionalBoxCode,
                                                     @Param("institutionalCapabilityCode") String institutionalCapabilityCode,
                                                     @Param("governanceChannel") String governanceChannel,
                                                     @Param("governanceScope") String governanceScope,
                                                     @Param("governanceRequired") Boolean governanceRequired,
                                                     @Param("governanceSatisfied") Boolean governanceSatisfied,
                                                     @Param("stepUpRequired") Boolean stepUpRequired,
                                                     @Param("stepUpSatisfied") Boolean stepUpSatisfied,
                                                     @Param("occurredAfter") LocalDateTime occurredAfter,
                                                     @Param("occurredBefore") LocalDateTime occurredBefore,
                                                     Pageable pageable);
}
