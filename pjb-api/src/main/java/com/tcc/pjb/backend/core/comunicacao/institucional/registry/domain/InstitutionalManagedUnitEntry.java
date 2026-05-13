package com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain;

import java.util.List;

public record InstitutionalManagedUnitEntry(
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
    public InstitutionalManagedUnitEntry {
        boxes = boxes == null ? List.of() : List.copyOf(boxes);
        laneCodes = laneCodes == null ? List.of() : List.copyOf(laneCodes);
        findings = findings == null ? List.of() : List.copyOf(findings);
    }
}
