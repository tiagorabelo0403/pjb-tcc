package com.tcc.pjb.backend.core.digitalizacao.domain;

import java.time.Instant;

public record DigitalizacaoJobWindowSnapshot(Long jobId, Instant startedAt, Instant completedAt, int totalPaginas) {}
