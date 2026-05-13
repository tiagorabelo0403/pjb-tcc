package com.tcc.pjb.backend.integration.datajud.feed.domain;

import java.time.Instant;

public record DataJudTribunalAuditEntry(String tribunalCodigo, long totalSent, Instant lastSentAt) {}
