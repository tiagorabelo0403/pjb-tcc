package com.tcc.pjb.backend.modules.advocacia.office.repository;

import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeProcessOperation;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdvOfficeProcessOperationRepository extends JpaRepository<AdvOfficeProcessOperation, Long> {

    @EntityGraph(attributePaths = {"processo", "equipe", "executor", "signer", "queueItem"})
    Optional<AdvOfficeProcessOperation> findWithGraphById(Long id);

    @EntityGraph(attributePaths = {"processo", "equipe", "executor", "signer", "queueItem"})
    Optional<AdvOfficeProcessOperation> findWithGraphByQueueItem_Id(Long queueItemId);

    @EntityGraph(attributePaths = {"processo", "equipe", "executor", "signer", "queueItem"})
    @Query("""
            select o from AdvOfficeProcessOperation o
            left join o.equipe e
            where o.actionType in :actionTypes
              and o.status in :statuses
              and ((:equipeId is not null and e.id = :equipeId) or (:equipeId is null and o.executor.id = :executorUserId))
            order by o.createdAt desc, o.id desc
            """)
    List<AdvOfficeProcessOperation> findDashboardPending(@Param("executorUserId") Long executorUserId,
                                                         @Param("equipeId") Long equipeId,
                                                         @Param("actionTypes") List<OfficeActionType> actionTypes,
                                                         @Param("statuses") List<String> statuses,
                                                         Pageable pageable);

    @Query("""
            select count(o) from AdvOfficeProcessOperation o
            left join o.equipe e
            where o.actionType in :actionTypes
              and o.status in :statuses
              and ((:equipeId is not null and e.id = :equipeId) or (:equipeId is null and o.executor.id = :executorUserId))
            """)
    long countDashboardPending(@Param("executorUserId") Long executorUserId,
                               @Param("equipeId") Long equipeId,
                               @Param("actionTypes") List<OfficeActionType> actionTypes,
                               @Param("statuses") List<String> statuses);

}
