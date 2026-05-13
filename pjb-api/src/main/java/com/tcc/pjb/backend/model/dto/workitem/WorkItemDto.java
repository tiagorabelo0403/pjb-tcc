package com.tcc.pjb.backend.model.dto.workitem;

import java.time.Instant;
import java.util.List;
import com.tcc.pjb.backend.model.dto.ui.UiHintDto;
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
public class WorkItemDto {
    private Long id;
    private Long processoId;
    private String processoNumero;
    private FaseProcessual faseOrigem;
    private String templateCode;
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
    private List<UiHintDto> uiHints;
}
