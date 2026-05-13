package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology;

import java.util.List;

public record NationalCommunicationInstitutionalManagedUnitUpsertRequest(
        String unitCode,
        String unitName,
        String parentUnitCode,
        String territorialScope,
        String municipalityCoverage,
        String defaultBoxCode,
        List<String> boxes,
        List<String> laneCodes,
        Boolean homologated,
        List<String> fundamentos
) {
}
