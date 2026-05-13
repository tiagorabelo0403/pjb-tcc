package com.tcc.pjb.backend.service.offline.domain;

import java.time.Instant;

public record OfflineReplayAuditView(
        String bundleToken,
        String status,
        Instant replayedAt,
        String summary
) {}
