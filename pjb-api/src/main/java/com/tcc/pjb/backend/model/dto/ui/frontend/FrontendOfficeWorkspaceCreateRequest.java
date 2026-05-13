package com.tcc.pjb.backend.model.dto.ui.frontend;

import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;

public record FrontendOfficeWorkspaceCreateRequest(
        @NotBlank String officeName,
        String mode,
        Boolean autoActivateOnLogin,
        Boolean allowPersonalOwnCases,
        Boolean allBrazilianLawEnabled,
        Set<RamoDireito> allowedRamos
) {
}
