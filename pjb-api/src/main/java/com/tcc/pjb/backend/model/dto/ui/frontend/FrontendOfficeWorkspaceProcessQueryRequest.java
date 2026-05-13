package com.tcc.pjb.backend.model.dto.ui.frontend;

import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;

public record FrontendOfficeWorkspaceProcessQueryRequest(
        Integer page,
        Integer size,
        String search,
        StatusProcesso status,
        RamoDireito ramoDireito,
        Boolean includePersonalOwnCases
) {
}
