package com.tcc.pjb.backend.model.dto.cidadao;

import java.util.List;

public record CidadaoGovHubRequisitosDto(
        boolean requiresGovBr,
        String minGovBrLevel,
        boolean stepUp,
        List<String> checklist
) {
}
