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
public class OfficePolicyDto {
    private Long equipeId;
    private boolean enabled;
    private Long signerUserId;
    private boolean bloqueiaCausasProprias;
    private boolean forcePatronoCertificate;
    private int minTrustAuto;
    private int maxAutoPorDia;
    private Set<RamoDireito> allowedRamos;
    private Set<OfficeActionType> autoActions;
}
