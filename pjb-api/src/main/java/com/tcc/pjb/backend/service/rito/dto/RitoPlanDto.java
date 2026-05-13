package com.tcc.pjb.backend.service.rito.dto;

import java.util.List;
import com.tcc.pjb.backend.model.dto.workitem.WorkItemDto;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RitoPlanDto {
    private Long processoId;
    private String numeroProcesso;
    private RitoProcessual rito;
    private FaseProcessual faseAtual;

    
    private List<FaseProcessual> allowedNext;

    
    private List<WorkItemDto> currentStageWork;

    
    private List<WorkItemDto> blockingOpen;
}
