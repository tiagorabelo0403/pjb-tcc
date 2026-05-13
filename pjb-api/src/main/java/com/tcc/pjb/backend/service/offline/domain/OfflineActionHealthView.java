package com.tcc.pjb.backend.service.offline.domain;

public record OfflineActionHealthView(
        String reference,
        String status,
        String summary
) {
}
