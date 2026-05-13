package com.tcc.pjb.backend.model.dto.workspace.localizador;

import java.util.List;
import java.util.UUID;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceLocalizadorCriteria {

    
    private String q;

    private Boolean somenteMeus;

    private Long jurisdicaoId;

    private List<StatusProcesso> status;

    private List<FaseProcessual> fases;

    private List<RitoProcessual> ritos;

    
    private List<UUID> etiquetaIds;
}
