package com.tcc.pjb.backend.core.criminal.custodia.domain;

import java.time.Instant;

public record MedidaCautelarSnapshot(Long medidaId,
                                     String tipo,
                                     boolean ativa,
                                     Instant proximoComparecimento) {
}
