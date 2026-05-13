package com.tcc.pjb.backend.integration.mni.domain;

public record MniPayloadHashQuery(
        String reference,
        String scope,
        Integer limit
) {
}
