package com.tcc.pjb.backend.core.observability.domain;

public record PjbSloTimelineHealthView(
        String reference,
        String status,
        String summary
) {
}
