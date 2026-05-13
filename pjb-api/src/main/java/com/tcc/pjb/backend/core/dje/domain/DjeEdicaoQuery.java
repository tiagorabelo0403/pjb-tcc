package com.tcc.pjb.backend.core.dje.domain;

public record DjeEdicaoQuery(
        String reference,
        String scope,
        Integer limit
) {
}
