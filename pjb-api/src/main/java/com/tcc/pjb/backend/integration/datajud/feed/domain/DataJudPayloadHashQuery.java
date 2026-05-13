package com.tcc.pjb.backend.integration.datajud.feed.domain;

public record DataJudPayloadHashQuery(
        String reference,
        String scope,
        Integer limit
) {
}
