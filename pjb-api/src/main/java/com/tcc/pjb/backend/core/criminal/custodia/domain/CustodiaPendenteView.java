package com.tcc.pjb.backend.core.criminal.custodia.domain;

import java.time.Instant;

public record CustodiaPendenteView(
        Long custodiaId,
        Long processoId,
        String presoNome,
        Instant dataPrisao,
        Instant prazoLimite24h,
        boolean vencida
) {
}
