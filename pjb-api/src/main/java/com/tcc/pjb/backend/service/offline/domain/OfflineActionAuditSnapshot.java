package com.tcc.pjb.backend.service.offline.domain;

import java.time.Instant;

public record OfflineActionAuditSnapshot(String bundleToken, int totalAcoes, int totalDecisorias, Instant generatedAt) {}
