package com.tcc.pjb.backend.integration.mni.domain;

public record MniProtocolHealthResult(
        boolean available,
        String summary,
        Long total
) {
}
