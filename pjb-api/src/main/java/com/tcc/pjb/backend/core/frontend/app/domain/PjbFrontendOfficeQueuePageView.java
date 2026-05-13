package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;

public record PjbFrontendOfficeQueuePageView(
        int page,
        int size,
        long totalElements,
        int totalPages,
        String status,
        List<PjbFrontendOfficeQueueItemView> items
) {
}
