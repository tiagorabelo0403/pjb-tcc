package com.tcc.pjb.backend.integration.datajud.feed.domain;
import java.time.Instant;
public record DataJudCheckpointAuditSnapshot(String tribunalCodigo, Long lastProcessoId, Long totalSent, Instant lastSentAt) {}
