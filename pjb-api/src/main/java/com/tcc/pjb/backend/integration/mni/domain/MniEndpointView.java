package com.tcc.pjb.backend.integration.mni.domain;

public record MniEndpointView(
        String tribunal,
        String endpoint,
        boolean enabled
) {}
