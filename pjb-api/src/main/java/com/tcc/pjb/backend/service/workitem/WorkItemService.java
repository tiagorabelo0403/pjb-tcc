package com.tcc.pjb.backend.service.workitem;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.scope.AcaoEscopo;
import com.tcc.pjb.backend.core.security.scope.PjbObjectScopeGuard;
import com.tcc.pjb.backend.core.security.scope.TipoObjetoProtegido;
import com.tcc.pjb.backend.model.dto.workitem.WorkItemDto;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.service.secretariat.projection.SecretariatQueueProjectionService;
import com.tcc.pjb.backend.service.ui.UiHintService;
import com.tcc.pjb.backend.service.ui.UiHistoryService;

@Service
public class WorkItemService {

    private final WorkItemRepository workItemRepository;
    private final CurrentUserService currentUserService;
    private final UiHintService uiHints;
    private final UiHistoryService uiHistory;
    private final SecretariatQueueProjectionService secretariatProjection;
    private final OutboxPublisher outbox;
    private final PjbObjectScopeGuard scopeGuard;

    public WorkItemService(
        WorkItemRepository workItemRepository,
        CurrentUserService currentUserService,
        UiHistoryService uiHistory,
        UiHintService uiHints,
        SecretariatQueueProjectionService secretariatProjection,
        OutboxPublisher outbox,
        PjbObjectScopeGuard scopeGuard
    ) {
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.uiHints = Objects.requireNonNull(uiHints);
        this.uiHistory = Objects.requireNonNull(uiHistory);
        this.secretariatProjection = Objects.requireNonNull(secretariatProjection);
        this.outbox = Objects.requireNonNull(outbox);
        this.scopeGuard = Objects.requireNonNull(scopeGuard);
    }

    public Page<WorkItemDto> inbox(int page, int size) {
        Usuario u = currentUserService.getRequired();

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 100));

        
        Page<WorkItem> byUser = workItemRepository.inboxByUser(u.getId(), pageable);
        if (!byUser.isEmpty()) {
            return byUser.map(this::toDto);
        }

        
        TipoUsuario role = u.getTipoUsuario();
        Page<WorkItem> byRole = workItemRepository.inboxByRoleAndTerritory(role, safe(u.getUf()), safe(u.getComarca()), pageable);
        return byRole.map(this::toDto);
    }

    public WorkItemDto get(Long id) {
        scopeGuard.requireAccess(TipoObjetoProtegido.WORK_ITEM, id, AcaoEscopo.LER);
        return workItemRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("WorkItem não encontrado: " + id));
    }

    @Transactional
    public WorkItemDto claim(Long id) {
        scopeGuard.requireAccess(TipoObjetoProtegido.WORK_ITEM, id, AcaoEscopo.CAPTURAR);
        Usuario u = currentUserService.getRequired();
        WorkItem wi = workItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("WorkItem não encontrado: " + id));

        if (wi.getAssignedUser() != null && !wi.getAssignedUser().getId().equals(u.getId())) {
            throw new IllegalStateException("Tarefa já atribuída a outro usuário.");
        }

        WorkItem before = snapshotForHistory(wi);

        wi.setAssignedUser(u);
        if (wi.getStatus() == WorkItemStatus.PENDENTE) {
            wi.setStatus(WorkItemStatus.EM_EXECUCAO);
        }
        WorkItem saved = workItemRepository.save(wi);
        refreshProjectionAndNotify(saved, "WORKITEM_CLAIMED");
        uiHistory.recordWorkItemChange(before, saved, "Tarefa assumida (claim)");
        return toDto(saved);
    }

    @Transactional
    public WorkItemDto done(Long id, String observacao) {
        scopeGuard.requireAccess(TipoObjetoProtegido.WORK_ITEM, id, AcaoEscopo.MOVIMENTAR);
        Usuario u = currentUserService.getRequired();
        WorkItem wi = workItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("WorkItem não encontrado: " + id));

        if (wi.getAssignedUser() != null && !wi.getAssignedUser().getId().equals(u.getId())) {
            throw new IllegalStateException("Somente o responsável pode concluir esta tarefa.");
        }

        WorkItem before = snapshotForHistory(wi);


        wi.setStatus(WorkItemStatus.CONCLUIDO);
        if (observacao != null && !observacao.isBlank()) {
            String prev = wi.getDescricao() != null ? wi.getDescricao() : "";
            wi.setDescricao(prev + "\n\n[OBS] " + observacao.trim());
        }
        WorkItem saved = workItemRepository.save(wi);
        refreshProjectionAndNotify(saved, "WORKITEM_DONE");
        uiHistory.recordWorkItemChange(before, saved, "Tarefa concluída (done)");
        return toDto(saved);
    }


    private WorkItem snapshotForHistory(WorkItem wi) {
        return WorkItem.builder()
            .id(wi.getId())
            .processo(wi.getProcesso())
            .type(wi.getType())
            .titulo(wi.getTitulo())
            .descricao(wi.getDescricao())
            .assignedRole(wi.getAssignedRole())
            .assignedUser(wi.getAssignedUser())
            .status(wi.getStatus())
            .prioridade(wi.getPrioridade())
            .blocking(wi.isBlocking())
            .dueAt(wi.getDueAt())
            .uf(wi.getUf())
            .comarca(wi.getComarca())
            .inboxKey(wi.getInboxKey())
            .build();
    }


    private WorkItemDto toDto(WorkItem w) {
        return WorkItemDto.builder()
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
                .uiHints(uiHints.hintsForWorkItem(w))
                .build();
    }

    private void refreshProjectionAndNotify(WorkItem w, String eventType) {
        try {
            
            secretariatProjection.upsert(w, computeScore(w), computeTags(w));
        } catch (Exception ignored) {
            
        }

        String inboxKey = safe(w.getInboxKey());
        if (inboxKey == null) {
            return;
        }

        Instant now = Instant.now();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", eventType);
        payload.put("workItemId", w.getId());
        payload.put("status", w.getStatus() != null ? w.getStatus().name() : null);
        payload.put("at", now.toString());

        outbox.enqueueSecretariatLive(
            inboxKey,
            "QUEUE_ITEM_UPDATED",
            now,
            Collections.unmodifiableMap(payload)
        );
    }

    private static String safe(String s) {
        if (s == null) return null;
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }

    private int computeScore(WorkItem w) {
        int score = 50;
        int p = w.getPrioridade() == null ? 3 : Math.max(1, w.getPrioridade());
        score += switch (p) {
            case 1 -> 60;
            case 2 -> 35;
            case 3 -> 15;
            default -> 5;
        };

        Instant now = Instant.now();
        if (w.getDueAt() != null) {
            if (w.getDueAt().isBefore(now)) {
                score += 80;
            } else {
                Duration d = Duration.between(now, w.getDueAt());
                if (d.compareTo(Duration.ofHours(24)) <= 0) score += 45;
                else if (d.compareTo(Duration.ofDays(3)) <= 0) score += 20;
            }
        }

        if (w.isBlocking()) score += 25;
        if (w.getStatus() == WorkItemStatus.CONCLUIDO) score -= 120;
        return Math.max(0, score);
    }

    private List<String> computeTags(WorkItem w) {
        List<String> tags = new ArrayList<>(8);

        if (w.getStatus() != null) {
            tags.add(w.getStatus().name());
        }

        int p = w.getPrioridade() == null ? 3 : w.getPrioridade();
        Instant now = Instant.now();
        if (w.getDueAt() != null && w.getDueAt().isBefore(now)) {
            tags.add("ATRASADO");
        } else if (p <= 1 || (w.getDueAt() != null && Duration.between(now, w.getDueAt()).compareTo(Duration.ofHours(24)) <= 0)) {
            tags.add("URGENTE");
        }

        if (w.isBlocking()) {
            tags.add("BLOQUEANTE");
        }

        if (w.getType() != null) {
            tags.add(w.getType().name());
            if (w.getType().name().contains("RECURSO")) {
                tags.add("RECURSO");
            }
        }

        Processo proc = w.getProcesso();
        if (proc != null) {
            if (proc.getNivelSigilo() != null && proc.getNivelSigilo() != NivelSigilo.PUBLICO) {
                tags.add("SIGILOSO");
            }
            String res = proc.getResultadoFinal();
            if (res != null && !res.isBlank()) {
                String t = res.trim().toUpperCase(Locale.ROOT);
                if (t.contains("PARCIAL")) tags.add("PARCIAL");
                else if (t.contains("IMPROCED")) tags.add("IMPROCEDENTE");
                else if (t.contains("PROCED")) tags.add("PROCEDENTE");
            }
        }

        return List.copyOf(tags);
    }
}
