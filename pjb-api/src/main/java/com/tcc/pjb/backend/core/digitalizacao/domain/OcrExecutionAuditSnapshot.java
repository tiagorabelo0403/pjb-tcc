package com.tcc.pjb.backend.core.digitalizacao.domain;

import java.time.Instant;

public record OcrExecutionAuditSnapshot(Long jobId, int totalPaginas, Instant executadoEm) {}
