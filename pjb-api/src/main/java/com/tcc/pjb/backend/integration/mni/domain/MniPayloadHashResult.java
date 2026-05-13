package com.tcc.pjb.backend.integration.mni.domain;

public record MniPayloadHashResult(
        boolean available,
        String summary,
        Long total
) {
}
