package com.tcc.pjb.backend.modules.custas.domain;

import java.time.Instant;

public record CustaStatusSnapshot(Long custaId, String status, Instant observadoEm) {}
