package com.tcc.pjb.backend.integration.mni.domain;

public record MniEndpointHealthQuery(
        String reference,
        String scope,
        Integer limit
) {
}
