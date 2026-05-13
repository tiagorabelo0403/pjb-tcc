package com.tcc.pjb.backend.core.criminal.custodia.domain;

import java.time.Instant;

public record CustodiaMedidaCautelarView(Long medidaId,
                                         Long processoId,
                                         String tipo,
                                         boolean ativa,
                                         Instant proximoComparecimento) {}
