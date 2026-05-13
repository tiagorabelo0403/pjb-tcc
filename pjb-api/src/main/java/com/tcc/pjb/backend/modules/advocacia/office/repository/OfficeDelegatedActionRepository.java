package com.tcc.pjb.backend.modules.advocacia.office.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.modules.advocacia.office.entity.OfficeDelegatedAction;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeDelegationMode;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeQueueStatus;
import com.tcc.pjb.backend.modules.advocacia.office.dto.OfficeDelegatedActionOpsDto;

public interface OfficeDelegatedActionRepository extends JpaRepository<OfficeDelegatedAction, Long> {

    interface DashboardRow {
        java.time.LocalDate getDay();
        Long getExecutorUserId();
        String getMode();
        Long getTotal();
    }

    @Query("select a from OfficeDelegatedAction a where a.equipe.id = :equipeId order by a.createdAt desc")
    Page<OfficeDelegatedAction> findByEquipe(@Param("equipeId") Long equipeId, Pageable pageable);

    @Query(
            "select new com.tcc.pjb.backend.modules.advocacia.office.dto.OfficeDelegatedActionOpsDto(" +
                    "a.id, a.createdAt, a.mode, a.actionType, a.executor.id, a.signer.id, a.resourceType, a.resourceId, q.status, a.jobId, a.requestId" +
                    ") " +
                    "from OfficeDelegatedAction a left join a.queueItem q " +
                    "where a.equipe.id = :equipeId " +
                    "and (:executorUserId is null or a.executor.id = :executorUserId) " +
                    "and (:signerUserId is null or a.signer.id = :signerUserId) " +
                    "and (:actionType is null or a.actionType = :actionType) " +
                    "and (:mode is null or a.mode = :mode) " +
                    "and (:queueStatus is null or q.status = :queueStatus) " +
                    "and (:resourceType is null or a.resourceType = :resourceType) " +
                    "and (:resourceId is null or a.resourceId = :resourceId) " +
                    "and (:fromTs is null or a.createdAt >= :fromTs) " +
                    "and (:toTs is null or a.createdAt <= :toTs) " +
                    "order by a.createdAt desc"
    )
    Page<OfficeDelegatedActionOpsDto> searchOps(
            @Param("equipeId") Long equipeId,
            @Param("executorUserId") Long executorUserId,
            @Param("signerUserId") Long signerUserId,
            @Param("actionType") OfficeActionType actionType,
            @Param("mode") OfficeDelegationMode mode,
            @Param("queueStatus") OfficeQueueStatus queueStatus,
            @Param("resourceType") String resourceType,
            @Param("resourceId") String resourceId,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs,
            Pageable pageable
    );

    @Query(
            value = "select cast(a.created_at as date) as day, a.executor_user_id as executorUserId, a.mode as mode, count(*) as total " +
                    "from adv_office_delegated_action a " +
                    "where a.equipe_id = :equipeId and a.signer_user_id = :signerUserId " +
                    "and (:fromTs is null or a.created_at >= :fromTs) " +
                    "and (:toTs is null or a.created_at <= :toTs) " +
                    "group by cast(a.created_at as date), a.executor_user_id, a.mode " +
                    "order by cast(a.created_at as date) desc",
            nativeQuery = true
    )
    List<DashboardRow> dashboard(@Param("equipeId") Long equipeId,
                                @Param("signerUserId") Long signerUserId,
                                @Param("fromTs") LocalDateTime fromTs,
                                @Param("toTs") LocalDateTime toTs);
}
