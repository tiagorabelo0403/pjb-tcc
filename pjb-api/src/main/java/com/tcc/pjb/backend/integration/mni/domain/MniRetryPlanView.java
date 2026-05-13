package com.tcc.pjb.backend.integration.mni.domain;

import java.time.Instant;

public record MniRetryPlanView(
        Long remessaId,
        int tentativas,
        int maxTentativas,
        Instant proximoRetryEm,
        String status
) {}
