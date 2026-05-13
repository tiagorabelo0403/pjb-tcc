package com.tcc.pjb.backend.model.dto.ui.frontend;

import com.tcc.pjb.backend.model.entity.enums.PapelEquipe;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record FrontendOfficeAffiliationInviteRequest(
        @NotNull Long equipeId,
        String invitedNome,
        String invitedEmail,
        String invitedCpf,
        String invitedOab,
        @NotNull PapelEquipe papelEquipe,
        String cargo,
        Boolean allowAllRamos,
        Set<RamoDireito> allowedRamos,
        Integer minTrustForAuto,
        Integer maxAutoPorDia,
        Boolean blockPersonalCases,
        Boolean autoActivateOnAccept,
        String modeOnAccept,
        Integer workspacePriority
) {
}
