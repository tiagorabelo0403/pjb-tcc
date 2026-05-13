package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OficialJusticaProcessoVinculoService {

    private final WorkItemRepository workItemRepository;

    public OficialJusticaProcessoVinculoService(WorkItemRepository workItemRepository) {
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
    }

    @Transactional(readOnly = true)
    public boolean possuiVinculoDireto(Long processoId, Long usuarioId, TipoUsuario tipoUsuario) {
        if (processoId == null || usuarioId == null || !isOficial(tipoUsuario)) {
            return false;
        }
        return vinculosDiretosProcesso(processoId, usuarioId, tipoUsuario, 80).stream().anyMatch(this::isVisibleOperationalLink);
    }

    @Transactional
    public List<WorkItem> vinculosDiretosProcesso(Long processoId, Long usuarioId, TipoUsuario tipoUsuario, int limit) {
        if (processoId == null || usuarioId == null || !isOficial(tipoUsuario)) {
            return List.of();
        }
        List<WorkItem> queried = workItemRepository.findByProcessoIdAndAssignedUserIdAndRolesAndStatusNot(
                processoId,
                usuarioId,
                rolesFor(tipoUsuario),
                WorkItemStatus.CANCELADO,
                PageRequest.of(0, safeLimit(limit))
        );
        return reconcileReappearance(queried).stream().filter(this::isVisibleOperationalLink).toList();
    }

    @Transactional
    public List<WorkItem> vinculosDiretosUsuario(Long usuarioId, TipoUsuario tipoUsuario, int limit) {
        if (usuarioId == null || !isOficial(tipoUsuario)) {
            return List.of();
        }
        List<WorkItem> queried = workItemRepository.findByAssignedUserIdAndRolesAndStatusNot(
                usuarioId,
                rolesFor(tipoUsuario),
                WorkItemStatus.CANCELADO,
                PageRequest.of(0, safeLimit(limit))
        );
        return reconcileReappearance(queried).stream().filter(this::isVisibleOperationalLink).toList();
    }

    private List<WorkItem> reconcileReappearance(List<WorkItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        ArrayList<WorkItem> dirty = new ArrayList<>();
        for (WorkItem item : items) {
            if (shouldReactivate(item)) {
                item.setSemInteresse(false);
                dirty.add(item);
            }
        }
        if (!dirty.isEmpty()) {
            workItemRepository.saveAll(dirty);
        }
        return List.copyOf(items);
    }

    private boolean shouldReactivate(WorkItem item) {
        return item != null
                && item.isSemInteresse()
                && item.getAssignedUser() != null
                && item.getAssignedUser().getId() != null
                && item.getStatus() != null
                && item.getStatus() != WorkItemStatus.CANCELADO
                && item.getStatus() != WorkItemStatus.CONCLUIDO
                && item.getProcesso() != null
                && (item.getProcesso().getStatusProcesso() == null || !item.getProcesso().getStatusProcesso().isEncerrado());
    }

    private static boolean isOficial(TipoUsuario tipoUsuario) {
        return tipoUsuario == TipoUsuario.OFICIAL_JUSTICA || tipoUsuario == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR;
    }

    private static List<TipoUsuario> rolesFor(TipoUsuario tipoUsuario) {
        if (tipoUsuario == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR) {
            return List.of(TipoUsuario.OFICIAL_JUSTICA_AVALIADOR, TipoUsuario.OFICIAL_JUSTICA);
        }
        return List.of(TipoUsuario.OFICIAL_JUSTICA, TipoUsuario.OFICIAL_JUSTICA_AVALIADOR);
    }

    private boolean isVisibleOperationalLink(WorkItem item) {
        return item != null
                && !item.isSemInteresse()
                && item.getProcesso() != null
                && (item.getProcesso().getStatusProcesso() == null || !item.getProcesso().getStatusProcesso().isEncerrado());
    }

    private static int safeLimit(int limit) {
        return Math.max(1, Math.min(limit, 200));
    }
}
