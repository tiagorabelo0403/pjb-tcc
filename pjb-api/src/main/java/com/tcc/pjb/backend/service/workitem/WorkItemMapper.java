package com.tcc.pjb.backend.service.workitem;

import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.dto.workitem.WorkItemDto;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;

@Component
public class WorkItemMapper {

    public WorkItemDto toDto(WorkItem w) {
        if (w == null) return null;
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
                .build();
    }
}
