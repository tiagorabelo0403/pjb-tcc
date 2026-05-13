package com.tcc.pjb.backend.integration.mni.domain;

public record MniEndpointHealthView(
        String reference,
        String status,
        String summary
) {
}
