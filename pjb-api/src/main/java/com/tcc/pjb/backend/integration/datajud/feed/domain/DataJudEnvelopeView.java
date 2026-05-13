package com.tcc.pjb.backend.integration.datajud.feed.domain;

import java.time.Instant;

public record DataJudEnvelopeView(
        String code,
        String status,
        String detail,
        Instant capturedAt,
        Long referenceId
) {
}
