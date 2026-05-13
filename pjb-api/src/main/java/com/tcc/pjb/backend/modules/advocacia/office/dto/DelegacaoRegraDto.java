package com.tcc.pjb.backend.modules.advocacia.office.dto;

import java.util.Set;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelegacaoRegraDto {
    private Long equipeId;
    private Long usuarioId;
    private boolean ativo;
    private boolean bloqueiaPessoal;
    private Integer minTrustAutoOverride;
    private Integer maxAutoPorDiaOverride;
    private Set<RamoDireito> allowedRamosOverride;
    private Set<OfficeActionType> autoActionsOverride;
}
