package com.tcc.pjb.backend.integration.mni.domain;

import java.time.Instant;

public record MniRetryWindowView(
        Long remessaId,
        Instant nextRetryAt,
        int tentativas,
        int maxTentativas
) {}
