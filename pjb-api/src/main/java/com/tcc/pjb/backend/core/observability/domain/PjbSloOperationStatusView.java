package com.tcc.pjb.backend.core.observability.domain;

public record PjbSloOperationStatusView(
        String reference,
        String status,
        String summary
) {
}
