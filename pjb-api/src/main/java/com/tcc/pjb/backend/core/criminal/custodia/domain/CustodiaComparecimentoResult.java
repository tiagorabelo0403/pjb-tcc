package com.tcc.pjb.backend.core.criminal.custodia.domain;

import java.time.Instant;

public record CustodiaComparecimentoResult(
        Long processoId,
        String tipoMedida,
        Instant proximoComparecimento,
        boolean ativo
) {}
