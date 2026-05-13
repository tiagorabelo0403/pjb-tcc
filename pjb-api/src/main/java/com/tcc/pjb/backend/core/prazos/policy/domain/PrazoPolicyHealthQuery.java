package com.tcc.pjb.backend.core.prazos.policy.domain;

public record PrazoPolicyHealthQuery(
        String reference,
        String scope,
        Integer limit
) {
}
