package com.tcc.pjb.backend.service.workspace;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.dto.workspace.fila.WorkspaceFilaWorkItemCriteria;
import com.tcc.pjb.backend.model.dto.workspace.fila.WorkspaceFilaWorkItemMode;
import com.tcc.pjb.backend.model.dto.workspace.fila.WorkspaceFilaWorkItemResumoResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;

@Service
public class WorkspaceFilaWorkItemQueryService {

    private final CurrentUserService currentUserService;
    private final EntityManager entityManager;
    private final PjbTimeService timeService;

    public WorkspaceFilaWorkItemQueryService(CurrentUserService currentUserService, EntityManager entityManager, PjbTimeService timeService) {
        this.currentUserService = currentUserService;
        this.entityManager = entityManager;
        this.timeService = timeService;
    }

    public Page<WorkspaceFilaWorkItemResumoResponse> listar(WorkspaceFilaWorkItemCriteria criteria, int page, int size) {
        Usuario u = currentUserService.getRequired();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 100));

        Instant now = timeService.nowUtc();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        
        CriteriaQuery<WorkItem> cq = cb.createQuery(WorkItem.class);
        Root<WorkItem> w = cq.from(WorkItem.class);

        
        w.fetch("processo", JoinType.LEFT);
        w.fetch("assignedUser", JoinType.LEFT);

        cq.select(w).distinct(true);

        List<Predicate> predicates = buildPredicates(criteria, u, cb, w, now);
        if (!predicates.isEmpty()) {
            cq.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        cq.orderBy(
                cb.asc(cb.selectCase().when(cb.isNull(w.get("dueAt")), 1).otherwise(0)),
                cb.asc(w.get("dueAt")),
                cb.asc(cb.coalesce(w.get("prioridade"), 3)),
                cb.asc(w.get("createdAt"))
        );

        TypedQuery<WorkItem> q = entityManager.createQuery(cq);
        q.setFirstResult((int) pageable.getOffset());
        q.setMaxResults(pageable.getPageSize());
        List<WorkItem> items = q.getResultList();

        
        long total = count(criteria, u, now);

        List<WorkspaceFilaWorkItemResumoResponse> content = items.stream()
                .map(it -> toResumo(it, now))
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    public long count(WorkspaceFilaWorkItemCriteria criteria) {
        Usuario u = currentUserService.getRequired();
        return count(criteria, u, timeService.nowUtc());
    }

    private long count(WorkspaceFilaWorkItemCriteria criteria, Usuario u, Instant now) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQ = cb.createQuery(Long.class);
        Root<WorkItem> w = countQ.from(WorkItem.class);
        countQ.select(cb.count(w.get("id")));

        List<Predicate> predicates = buildPredicates(criteria, u, cb, w, now);
        if (!predicates.isEmpty()) {
            countQ.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        Long total = entityManager.createQuery(countQ).getSingleResult();
        return total == null ? 0 : total;
    }

    private List<Predicate> buildPredicates(WorkspaceFilaWorkItemCriteria criteria,
                                           Usuario u,
                                           CriteriaBuilder cb,
                                           Root<WorkItem> w,
                                           Instant now) {
        List<Predicate> predicates = new ArrayList<>();

        
        Join<WorkItem, Usuario> assignedUser = w.join("assignedUser", JoinType.LEFT);

        
        
        
        TipoUsuario role = u.getTipoUsuario();
        Predicate byUser = cb.equal(assignedUser.get("id"), u.getId());
        Predicate byPool = cb.and(
                cb.isNull(w.get("assignedUser")),
                role != null ? cb.equal(w.get("assignedRole"), role) : cb.disjunction(),
                territory(cb, w, u)
        );
        predicates.add(cb.or(byUser, byPool));

        
        List<WorkItemStatus> statuses = (criteria != null && criteria.getStatus() != null && !criteria.getStatus().isEmpty())
                ? criteria.getStatus()
                : List.of(WorkItemStatus.PENDENTE, WorkItemStatus.EM_EXECUCAO);
        predicates.add(w.get("status").in(statuses));

        if (criteria == null) {
            return predicates;
        }

        if (Boolean.TRUE.equals(criteria.getBlockingOnly())) {
            predicates.add(cb.isTrue(w.get("blocking")));
        }

        if (criteria.getTypes() != null && !criteria.getTypes().isEmpty()) {
            predicates.add(w.get("type").in(criteria.getTypes()));
        }

        if (criteria.getMaxPrioridade() != null) {
            predicates.add(cb.lessThanOrEqualTo(cb.coalesce(w.get("prioridade"), 3), criteria.getMaxPrioridade()));
        }

        WorkspaceFilaWorkItemMode mode = criteria.getMode() != null ? criteria.getMode() : WorkspaceFilaWorkItemMode.AUTO_INBOX;
        if (mode == WorkspaceFilaWorkItemMode.DUE_WITHIN_HOURS) {
            int hours = criteria.getHours() != null ? criteria.getHours() : 48;
            Instant limit = now.plus(Math.max(1, Math.min(hours, 24 * 30)), ChronoUnit.HOURS);
            Path<Instant> duePath = w.get("dueAt");
            predicates.add(cb.isNotNull(duePath));
            predicates.add(cb.lessThanOrEqualTo(duePath, limit));
            if (!Boolean.TRUE.equals(criteria.getIncludeOverdue())) {
                predicates.add(cb.greaterThanOrEqualTo(duePath, now));
            }
        }

        return predicates;
    }

    private Predicate territory(CriteriaBuilder cb, Root<WorkItem> w, Usuario u) {
        String uf = safe(u.getUf());
        String comarca = safe(u.getComarca());
        Predicate p1 = (uf == null) ? cb.conjunction() : cb.equal(w.get("uf"), uf);
        Predicate p2 = (comarca == null) ? cb.conjunction() : cb.equal(w.get("comarca"), comarca);
        return cb.and(p1, p2);
    }

    private static String safe(String v) {
        if (v == null) return null;
        String s = v.trim();
        return s.isBlank() ? null : s;
    }

    private WorkspaceFilaWorkItemResumoResponse toResumo(WorkItem w, Instant now) {
        Instant due = w.getDueAt();
        String bucket;
        if (due == null) {
            bucket = "NO_DUE";
        } else if (due.isBefore(now)) {
            bucket = "OVERDUE";
        } else if (due.isBefore(now.plus(24, ChronoUnit.HOURS))) {
            bucket = "DUE_24H";
        } else if (due.isBefore(now.plus(48, ChronoUnit.HOURS))) {
            bucket = "DUE_48H";
        } else {
            bucket = "FUTURE";
        }

        return WorkspaceFilaWorkItemResumoResponse.builder()
                .id(w.getId())
                .processoId(w.getProcesso() != null ? w.getProcesso().getId() : null)
                .processoNumero(w.getProcesso() != null ? w.getProcesso().getNumeroUnificado() : null)
                .faseOrigem(w.getFaseOrigem())
                .type(w.getType())
                .titulo(w.getTitulo())
                .descricao(w.getDescricao())
                .assignedRole(w.getAssignedRole())
                .assignedUserId(w.getAssignedUser() != null ? w.getAssignedUser().getId() : null)
                .status(w.getStatus())
                .prioridade(w.getPrioridade())
                .blocking(w.isBlocking())
                .dueAt(w.getDueAt())
                .uf(w.getUf())
                .comarca(w.getComarca())
                .slaBucket(bucket)
                .build();
    }
}
