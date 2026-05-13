package com.tcc.pjb.backend.model.dto.workspace.fila;

import java.util.List;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceFilaWorkItemCriteria {

    @Builder.Default
    private WorkspaceFilaWorkItemMode mode = WorkspaceFilaWorkItemMode.AUTO_INBOX;

    
    private Boolean blockingOnly;

    
    private List<WorkItemType> types;

    
    private List<WorkItemStatus> status;

    
    @Min(1)
    @Max(720)
    private Integer hours;

    @Builder.Default
    private Boolean includeOverdue = Boolean.TRUE;

    
    @Min(1)
    @Max(5)
    private Integer maxPrioridade;
}
