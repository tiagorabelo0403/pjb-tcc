package com.tcc.pjb.backend.core.frontend.readiness.domain;

public record PjbFrontendDtoContractView(
        String typeName,
        String packageName,
        String category,
        boolean apiEnvelope,
        boolean recordType,
        boolean stableForFrontend
) {
}
