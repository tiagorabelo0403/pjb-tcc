package com.tcc.pjb.backend.core.digitalizacao.domain;

import java.time.Instant;

public record DigitalizacaoReviewQueueAuditSnapshot(Long jobId,
                                                    int totalPendentes,
                                                    Instant generatedAt) {}
