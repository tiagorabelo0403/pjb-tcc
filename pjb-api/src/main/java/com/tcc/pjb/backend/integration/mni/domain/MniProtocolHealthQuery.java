package com.tcc.pjb.backend.integration.mni.domain;

public record MniProtocolHealthQuery(
        String reference,
        String scope,
        Integer limit
) {
}
