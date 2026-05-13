package com.tcc.pjb.backend.core.observability.domain;

public record PjbSloRegistryStatusView(
        String reference,
        String status,
        String summary
) {
}
