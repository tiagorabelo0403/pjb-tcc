package com.tcc.pjb.backend.model.dto.workspace.fila;

import java.time.Instant;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceFilaWorkItemResumoResponse {
    private Long id;
    private Long processoId;
    private String processoNumero;
    private FaseProcessual faseOrigem;
    private WorkItemType type;
    private String titulo;
    private String descricao;
    private TipoUsuario assignedRole;
    private Long assignedUserId;
    private WorkItemStatus status;
    private Integer prioridade;
    private boolean blocking;
    private Instant dueAt;
    private String uf;
    private String comarca;

    
    private String slaBucket;
}
