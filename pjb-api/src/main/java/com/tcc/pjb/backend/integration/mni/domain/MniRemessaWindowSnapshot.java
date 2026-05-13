package com.tcc.pjb.backend.integration.mni.domain;

import java.time.Instant;

public record MniRemessaWindowSnapshot(Long remessaId, Instant proximoRetryEm, int tentativas, int maxTentativas) {}
