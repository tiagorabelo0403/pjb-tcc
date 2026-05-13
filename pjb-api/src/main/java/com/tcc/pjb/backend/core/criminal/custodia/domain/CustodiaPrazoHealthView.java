package com.tcc.pjb.backend.core.criminal.custodia.domain;

import java.time.Instant;

public record CustodiaPrazoHealthView(
        Long custodiaId,
        Instant prazoLimite24h,
        boolean overdue
) {}
