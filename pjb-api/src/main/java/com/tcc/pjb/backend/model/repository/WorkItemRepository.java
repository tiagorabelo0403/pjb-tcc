package com.tcc.pjb.backend.model.repository;

import java.time.Instant;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;

public interface WorkItemRepository extends JpaRepository<WorkItem, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select w from WorkItem w
            left join fetch w.processo p
            left join fetch w.assignedUser u
            where w.id = :id
            """)
    Optional<WorkItem> findLockedDetailedById(@Param("id") Long id);


    @Query("""
            select w from WorkItem w
            where w.processo.id = :processoId
              and w.templateCode = :templateCode
              and w.status <> :status
            order by w.createdAt desc, w.id desc
            """)
    List<WorkItem> findByProcessoIdAndTemplateCodeAndStatusNotOrderByCreatedAtDescIdDesc(@Param("processoId") Long processoId,
                                                                                          @Param("templateCode") String templateCode,
                                                                                          @Param("status") WorkItemStatus status,
                                                                                          Pageable pageable);

    default Optional<WorkItem> findFirstByProcesso_IdAndTemplateCodeAndStatusNot(Long processoId, String templateCode, WorkItemStatus status) {
        return findByProcessoIdAndTemplateCodeAndStatusNotOrderByCreatedAtDescIdDesc(processoId, templateCode, status, PageRequest.of(0, 1)).stream().findFirst();
    }

    List<WorkItem> findAllByProcesso_IdAndTemplateCodeInAndStatusNot(Long processoId, List<String> templateCodes, WorkItemStatus status);

    @Query("""
            select w from WorkItem w
            where w.processo.id = :processoId
              and w.templateCode = :templateCode
            order by w.createdAt desc, w.id desc
            """)
    List<WorkItem> findByProcessoIdAndTemplateCodeOrderByCreatedAtDescIdDesc(@Param("processoId") Long processoId,
                                                                              @Param("templateCode") String templateCode,
                                                                              Pageable pageable);

    default Optional<WorkItem> findLatestByProcessoIdAndTemplateCode(Long processoId, String templateCode) {
        return findByProcessoIdAndTemplateCodeOrderByCreatedAtDescIdDesc(processoId, templateCode, PageRequest.of(0, 1)).stream().findFirst();
    }

    List<WorkItem> findAllByProcesso_IdInAndTemplateCodeIn(List<Long> processoIds, List<String> templateCodes);

    @Query("""
            select w from WorkItem w
            where w.processo.id = :processoId
              and w.status <> 'CANCELADO'
            order by w.createdAt asc
            """)
    List<WorkItem> findAllByProcesso(@Param("processoId") Long processoId);


    Optional<WorkItem> findFirstByProcesso_IdAndStatusNotOrderByCreatedAtDescIdDesc(Long processoId, WorkItemStatus status);

    @Query("""
            select count(w) from WorkItem w
            where w.processo.id = :processoId
              and w.status in ('PENDENTE','EM_EXECUCAO')
            """)
    long countOpenByProcesso(@Param("processoId") Long processoId);

    @Query("""
            select count(w) from WorkItem w
            where w.processo.id = :processoId
              and w.blocking = true
              and w.status in ('PENDENTE','EM_EXECUCAO')
            """)
    long countOpenBlockingByProcesso(@Param("processoId") Long processoId);

    @Query("""
            select w from WorkItem w
            where w.processo.id = :processoId
              and w.faseOrigem = :fase
              and w.blocking = true
              and w.status <> 'CONCLUIDO'
              and w.status <> 'CANCELADO'
            """)
    List<WorkItem> findBlockingOpen(@Param("processoId") Long processoId, @Param("fase") FaseProcessual fase);

    @EntityGraph(attributePaths = {"processo", "assignedUser"})
    @Query("""
            select w from WorkItem w
            where (w.assignedUser.id = :userId)
              and w.status in ('PENDENTE','EM_EXECUCAO')
            order by case when w.dueAt is null then 1 else 0 end, w.dueAt asc, w.prioridade asc, w.createdAt asc
            """)
    Page<WorkItem> inboxByUser(@Param("userId") Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"processo", "assignedUser"})
    @Query("""
            select w from WorkItem w
            where (w.assignedUser is null)
              and (w.assignedRole = :role)
              and w.status in ('PENDENTE','EM_EXECUCAO')
              and (:uf is null or w.uf = :uf)
              and (:comarca is null or w.comarca = :comarca)
            order by case when w.dueAt is null then 1 else 0 end, w.dueAt asc, w.prioridade asc, w.createdAt asc
            """)
    Page<WorkItem> inboxByRoleAndTerritory(@Param("role") TipoUsuario role,
                                          @Param("uf") String uf,
                                          @Param("comarca") String comarca,
                                          Pageable pageable);

    @Query("""
            select count(w) from WorkItem w
            where w.processo.id = :processoId
              and w.blocking = true
              and w.assignedRole = :role
              and w.status in ('PENDENTE','EM_EXECUCAO')
            """)
    long countOpenBlockingByProcessAndRole(@Param("processoId") Long processoId, @Param("role") TipoUsuario role);

    @Query("""
            select min(w.dueAt) from WorkItem w
            where w.processo.id = :processoId
              and w.status in ('PENDENTE','EM_EXECUCAO')
            """)
    Instant minDueAtOpen(@Param("processoId") Long processoId);

    


    @EntityGraph(attributePaths = {"processo", "assignedUser"})
    @Query("""
            select w from WorkItem w
            where w.assignedUser.id = :userId
              and w.assignedRole in :roles
              and w.status <> :status
            order by w.updatedAt desc, w.createdAt desc, w.id desc
            """)
    List<WorkItem> findByAssignedUserIdAndRolesAndStatusNot(@Param("userId") Long userId,
                                                            @Param("roles") List<TipoUsuario> roles,
                                                            @Param("status") WorkItemStatus status,
                                                            Pageable pageable);

    @EntityGraph(attributePaths = {"processo", "assignedUser"})
    @Query("""
            select w from WorkItem w
            where w.processo.id = :processoId
              and w.assignedUser.id = :userId
              and w.assignedRole in :roles
              and w.status <> :status
            order by w.updatedAt desc, w.createdAt desc, w.id desc
            """)
    List<WorkItem> findByProcessoIdAndAssignedUserIdAndRolesAndStatusNot(@Param("processoId") Long processoId,
                                                                         @Param("userId") Long userId,
                                                                         @Param("roles") List<TipoUsuario> roles,
                                                                         @Param("status") WorkItemStatus status,
                                                                         Pageable pageable);

    @Query("""
            select count(w) from WorkItem w
            where w.processo.id = :processoId
              and w.assignedUser.id = :userId
              and w.assignedRole in :roles
              and w.status <> :status
            """)
    long countByProcessoIdAndAssignedUserIdAndRolesAndStatusNot(@Param("processoId") Long processoId,
                                                                @Param("userId") Long userId,
                                                                @Param("roles") List<TipoUsuario> roles,
                                                                @Param("status") WorkItemStatus status);

    @EntityGraph(attributePaths = {"processo", "assignedUser"})
    @Query("""
        SELECT w
        FROM WorkItem w
        WHERE w.assignedUser.id = :userId
          AND w.dueAt IS NOT NULL
          AND w.dueAt <= :limit
          AND (w.status IS NULL OR w.status <> 'CONCLUIDO')
        ORDER BY w.dueAt ASC
    """)
    List<WorkItem> findDueByAssignedUser(
            @Param("userId") Long userId,
            @Param("limit") Instant limit,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"processo", "assignedUser"})
    @Query("""
        SELECT w
        FROM WorkItem w
        WHERE w.assignedRole = :role
          AND (:uf IS NULL OR w.uf = :uf)
          AND (:comarca IS NULL OR w.comarca = :comarca)
          AND w.dueAt IS NOT NULL
          AND w.dueAt <= :limit
          AND (w.status IS NULL OR w.status <> 'CONCLUIDO')
        ORDER BY w.dueAt ASC
    """)
    List<WorkItem> findDueByRoleAndTerritory(
            @Param("role") TipoUsuario role,
            @Param("uf") String uf,
            @Param("comarca") String comarca,
            @Param("limit") Instant limit,
            Pageable pageable
    );

    @Query("""
        SELECT count(w)
        FROM WorkItem w
        WHERE w.assignedUser.id = :userId
          AND w.dueAt IS NOT NULL
          AND w.dueAt <= :limit
          AND (w.status IS NULL OR w.status <> 'CONCLUIDO')
    """)
    long countDueByAssignedUser(@Param("userId") Long userId, @Param("limit") Instant limit);

    @Query("""
        SELECT count(w)
        FROM WorkItem w
        WHERE w.assignedUser.id = :userId
          AND w.dueAt IS NOT NULL
          AND w.dueAt < :now
          AND (w.status IS NULL OR w.status <> 'CONCLUIDO')
    """)
    long countOverdueByAssignedUser(@Param("userId") Long userId, @Param("now") Instant now);

    @Query("""
        SELECT MIN(w.dueAt)
        FROM WorkItem w
        WHERE w.processo.id = :processoId
          AND w.dueAt IS NOT NULL
          AND (w.status IS NULL OR w.status <> 'CONCLUIDO')
    """)
    Instant minOpenDueAtForProcesso(@Param("processoId") Long processoId);




    @Query("""
            select w from WorkItem w
            where w.inboxKey = :inboxKey
              and w.templateCode like 'SECRETARIA:RETORNO_CUMPRIMENTO_OFICIAL:%'
              and w.status in :statuses
            order by case when w.dueAt is null then 1 else 0 end, w.dueAt asc, w.prioridade asc, w.updatedAt desc, w.id desc
            """)
    List<WorkItem> findSecretariatOfficialOutcomeItemsByInbox(@Param("inboxKey") String inboxKey,
                                                              @Param("statuses") List<WorkItemStatus> statuses,
                                                              Pageable pageable);

    @Query("""
            select count(w) from WorkItem w
            where w.inboxKey = :inboxKey
              and w.templateCode like 'SECRETARIA:RETORNO_CUMPRIMENTO_OFICIAL:%'
              and w.status in :statuses
            """)
    long countSecretariatOfficialOutcomeItemsByInbox(@Param("inboxKey") String inboxKey,
                                                     @Param("statuses") List<WorkItemStatus> statuses);

    @Query("""
            select w from WorkItem w
            where w.processo.id = :processoId
              and w.assignedUser is not null
              and (w.assignedRole in :roles or w.assignedUser.tipoUsuario in :roles)
              and w.status <> 'CANCELADO'
            order by coalesce(w.updatedAt, w.createdAt) desc, w.createdAt desc, w.id desc
            """)
    List<WorkItem> findLatestLinkedOfficialByProcesso(@Param("processoId") Long processoId,
                                                      @Param("roles") List<TipoUsuario> roles,
                                                      Pageable pageable);

    default Optional<WorkItem> findLatestLinkedOfficialByProcesso(Long processoId, List<TipoUsuario> roles) {
        if (processoId == null || roles == null || roles.isEmpty()) {
            return Optional.empty();
        }
        return findLatestLinkedOfficialByProcesso(processoId, roles, PageRequest.of(0, 1)).stream().findFirst();
    }

    @Query("""
        select w
        from WorkItem w
        where w.templateCode = :templateCode
        order by w.createdAt desc, w.id desc
    """)
    List<WorkItem> findByTemplateCodeOrderByCreatedAtDescIdDesc(@Param("templateCode") String templateCode, Pageable pageable);

    default Optional<WorkItem> findByTemplateCode(String templateCode) {
        return findByTemplateCodeOrderByCreatedAtDescIdDesc(templateCode, PageRequest.of(0, 1)).stream().findFirst();
    }

    long countByStatus(WorkItemStatus status);


    @Query("""
            select w from WorkItem w
            where w.assignedUser is not null
              and (w.assignedRole in :roles or w.assignedUser.tipoUsuario in :roles)
              and (w.createdAt >= :eventAfter or w.updatedAt >= :eventAfter)
              and w.status in ('PENDENTE','EM_EXECUCAO')
            order by w.updatedAt desc, w.createdAt desc, w.id desc
            """)
    List<WorkItem> findRecentAssignedUsersByRoles(@Param("roles") List<TipoUsuario> roles,
                                                  @Param("eventAfter") Instant eventAfter,
                                                  Pageable pageable);


    @EntityGraph(attributePaths = {"processo", "assignedUser"})
    @Query("""
            select w from WorkItem w
            where w.assignedUser.id = :userId
              and (w.assignedRole in :roles or w.assignedUser.tipoUsuario in :roles)
              and (w.createdAt >= :eventAfter or w.updatedAt >= :eventAfter)
              and w.status in ('PENDENTE','EM_EXECUCAO')
            order by w.updatedAt desc, w.createdAt desc, w.id desc
            """)
    List<WorkItem> findRecentAssignedActiveByUserAndRoles(@Param("userId") Long userId,
                                                          @Param("roles") List<TipoUsuario> roles,
                                                          @Param("eventAfter") Instant eventAfter,
                                                          Pageable pageable);


    @Query("""
            select w from WorkItem w
            where w.inboxKey = :inboxKey
              and w.templateCode like 'BALCAO_OFICIAL_AUTODESTINO:%'
              and w.status in :statuses
            order by case when w.dueAt is null then 1 else 0 end, w.dueAt asc, w.updatedAt desc, w.id desc
            """)
    List<WorkItem> findOfficialReturnDeskItemsByInbox(@Param("inboxKey") String inboxKey,
                                                      @Param("statuses") List<WorkItemStatus> statuses,
                                                      Pageable pageable);

    @Query("""
            select count(w) from WorkItem w
            where w.inboxKey = :inboxKey
              and w.templateCode like 'BALCAO_OFICIAL_AUTODESTINO:%'
              and w.status in :statuses
            """)
    long countOfficialReturnDeskItemsByInbox(@Param("inboxKey") String inboxKey,
                                             @Param("statuses") List<WorkItemStatus> statuses);

    long countByStatusAndDueAtBetween(WorkItemStatus status, Instant start, Instant end);

    long countByStatusAndDueAtBefore(WorkItemStatus status, Instant before);

    long countByCreatedAtAfter(Instant instant);

    long countByUfAndStatus(String uf, WorkItemStatus status);

    long countByUfAndComarcaAndStatus(String uf, String comarca, WorkItemStatus status);

    @Query("""
            select w from WorkItem w
            join fetch w.processo p
            left join fetch w.assignedUser u
            where w.inboxKey like concat(:inboxPrefix, '%')
              and (:uf is null or w.uf = :uf)
              and (:comarca is null or w.comarca = :comarca)
              and w.status in :statuses
            order by case when w.dueAt is null then 1 else 0 end,
                     coalesce(w.dueAt, w.updatedAt, w.createdAt) asc,
                     w.prioridade asc,
                     w.createdAt asc,
                     w.id asc
            """)
    List<WorkItem> findActiveByInboxPrefixAndTerritory(@Param("inboxPrefix") String inboxPrefix,
                                                       @Param("uf") String uf,
                                                       @Param("comarca") String comarca,
                                                       @Param("statuses") List<WorkItemStatus> statuses,
                                                       Pageable pageable);

    @Query("""
            select count(w) from WorkItem w
            where w.inboxKey like concat(:inboxPrefix, '%')
              and (:uf is null or w.uf = :uf)
              and (:comarca is null or w.comarca = :comarca)
              and w.status in :statuses
            """)
    long countActiveByInboxPrefixAndTerritory(@Param("inboxPrefix") String inboxPrefix,
                                              @Param("uf") String uf,
                                              @Param("comarca") String comarca,
                                              @Param("statuses") List<WorkItemStatus> statuses);

    @Query("""
            select w from WorkItem w
            join fetch w.processo p
            left join fetch w.assignedUser u
            where w.inboxKey like concat(:inboxPrefix, '%')
              and (:uf is null or w.uf = :uf)
              and (:comarca is null or w.comarca = :comarca)
              and coalesce(w.dueAt, w.updatedAt, w.createdAt) between :from and :to
              and w.status in :statuses
            order by case when w.dueAt is null then 1 else 0 end,
                     coalesce(w.dueAt, w.updatedAt, w.createdAt) asc,
                     w.prioridade asc,
                     w.createdAt asc,
                     w.id asc
            """)
    List<WorkItem> findCalendarWindowByInboxPrefixAndTerritory(@Param("inboxPrefix") String inboxPrefix,
                                                               @Param("uf") String uf,
                                                               @Param("comarca") String comarca,
                                                               @Param("from") Instant from,
                                                               @Param("to") Instant to,
                                                               @Param("statuses") List<WorkItemStatus> statuses,
                                                               Pageable pageable);


    @Query("""
        select count(w)
        from WorkItem w
        where w.assignedUser.id = :userId
          and w.status = 'CONCLUIDO'
          and w.updatedAt is not null
          and w.updatedAt >= :from
    """)
    long countCompletedByAssignedUserAfter(@Param("userId") Long userId, @Param("from") Instant from);

    @EntityGraph(attributePaths = {"assignedUser"})
    @Query("""
        select w from WorkItem w
        where w.inboxKey = :inboxKey
          and w.status = 'CONCLUIDO'
          and w.updatedAt is not null
          and w.updatedAt >= :from
          and w.assignedUser is not null
        order by w.updatedAt desc
    """)
    List<WorkItem> findConcluidosPorInboxAposData(@Param("inboxKey") String inboxKey,
                                                  @Param("from") Instant from,
                                                  Pageable pageable);

    @Query("""
        select w.assignedUser.id,
               w.assignedUser.nome,
               count(w),
               coalesce(sum(case when w.dueAt is not null and w.dueAt < :now then 1 else 0 end), 0),
               coalesce(sum(case when w.blocking = true then 1 else 0 end), 0),
               min(w.dueAt)
        from WorkItem w
        where w.inboxKey = :inboxKey
          and w.status in ('PENDENTE','EM_EXECUCAO')
        group by w.assignedUser.id, w.assignedUser.nome
        order by count(w) desc
    """)
    List<Object[]> radarByInboxAssignedUser(@Param("inboxKey") String inboxKey, @Param("now") Instant now);


    Page<WorkItem> findByStatusAndDueAtBefore(WorkItemStatus status, Instant before, Pageable pageable);

    Page<WorkItem> findByQueueCodeAndStatusAndDueAtBefore(String queueCode, WorkItemStatus status, Instant before, Pageable pageable);



    @Query("""
            select w from WorkItem w
            where w.inboxKey = :inboxKey
              and w.templateCode like 'SECRETARIA:GAVETA_OFICIO_OFICIAL:%'
              and w.status in :statuses
            order by coalesce(w.updatedAt, w.createdAt) desc, w.createdAt desc, w.id desc
            """)
    List<WorkItem> findSecretariatOfficialDrawerItemsByInbox(@Param("inboxKey") String inboxKey,
                                                             @Param("statuses") List<WorkItemStatus> statuses,
                                                             Pageable pageable);

    @Query("""
            select count(w) from WorkItem w
            where w.inboxKey = :inboxKey
              and w.templateCode like 'SECRETARIA:GAVETA_OFICIO_OFICIAL:%'
              and w.status in :statuses
            """)
    long countSecretariatOfficialDrawerItemsByInbox(@Param("inboxKey") String inboxKey,
                                                    @Param("statuses") List<WorkItemStatus> statuses);

    @Query("""
        select w.queueCode
        from WorkItem w
        where w.queueCode is not null and w.queueCode <> ''
        group by w.queueCode
        having count(w) > :threshold
        order by count(w) desc
    """)
    List<String> findFilasComMaisDe(@Param("threshold") long threshold);


    @EntityGraph(attributePaths = {"processo", "assignedUser"})
    @Query("""
            select w from WorkItem w
            where w.assignedUser.id = :userId
              and coalesce(w.dueAt, w.updatedAt, w.createdAt) between :from and :to
              and w.status in ('PENDENTE','EM_EXECUCAO')
            order by case when w.dueAt is null then 1 else 0 end,
                     coalesce(w.dueAt, w.updatedAt, w.createdAt) asc,
                     w.prioridade asc,
                     w.createdAt asc,
                     w.id asc
            """)
    List<WorkItem> findCalendarWindowByAssignedUser(@Param("userId") Long userId,
                                                    @Param("from") Instant from,
                                                    @Param("to") Instant to,
                                                    Pageable pageable);

    @Query("""
            select w from WorkItem w
            where w.assignedRole = :role
              and w.assignedUser is null
              and (:uf is null or w.uf = :uf)
              and (:comarca is null or w.comarca = :comarca)
              and coalesce(w.dueAt, w.updatedAt, w.createdAt) between :from and :to
              and w.status in ('PENDENTE','EM_EXECUCAO')
            order by case when w.dueAt is null then 1 else 0 end,
                     coalesce(w.dueAt, w.updatedAt, w.createdAt) asc,
                     w.prioridade asc,
                     w.createdAt asc,
                     w.id asc
            """)
    List<WorkItem> findCalendarWindowByRoleAndTerritory(@Param("role") TipoUsuario role,
                                                        @Param("uf") String uf,
                                                        @Param("comarca") String comarca,
                                                        @Param("from") Instant from,
                                                        @Param("to") Instant to,
                                                        Pageable pageable);

    @EntityGraph(attributePaths = {"processo"})
    @Query("""
            select w from WorkItem w
            where w.templateCode like concat('HONORARIO:%:', :usuarioId)
            order by w.createdAt desc, w.id desc
            """)
    List<WorkItem> findHonorariosPericiaisPorSolicitante(@Param("usuarioId") Long usuarioId);

}
