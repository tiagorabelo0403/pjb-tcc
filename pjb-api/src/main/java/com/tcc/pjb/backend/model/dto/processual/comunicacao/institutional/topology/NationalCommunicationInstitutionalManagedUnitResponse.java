package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology;

import java.util.List;

public record NationalCommunicationInstitutionalManagedUnitResponse(
        String unitCode,
        String unitName,
        String parentUnitCode,
        String territorialScope,
        String municipalityCoverage,
        String defaultBoxCode,
        String workPartition,
        String readReplicaCode,
        boolean managed,
        boolean homologated,
        List<String> boxes,
        List<String> laneCodes,
        List<String> findings
) {
}
