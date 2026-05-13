package com.tcc.pjb.backend.core.criminal.custodia.domain;

import java.time.Instant;

public record MedidaCautelarWindowView(
        Long processoId,
        String tipo,
        Instant proximoComparecimento,
        boolean ativa
) {}
