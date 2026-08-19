package com.tcc.pjb.backend.modules.custas.domain;

import java.time.LocalDate;

public record CustaEmissionResult(
        Long custaId,
        String tipo,
        String status,
        LocalDate vencimento,
        boolean isento
) {}
