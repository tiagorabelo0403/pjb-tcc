package com.tcc.pjb.backend.model.dto.admin;

import java.util.List;

public record AdminInstitutionalVisibilitySimulationResponse(
        String tierCode,
        String tierLabel,
        boolean allowed,
        boolean auditRequired,
        boolean timeBound,
        List<String> reasons,
        List<String> restrictions
) {
}
