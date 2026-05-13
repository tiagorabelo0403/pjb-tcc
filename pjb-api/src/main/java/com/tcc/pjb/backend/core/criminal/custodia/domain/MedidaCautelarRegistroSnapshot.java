package com.tcc.pjb.backend.core.criminal.custodia.domain;

import java.time.Instant;

public record MedidaCautelarRegistroSnapshot(Long id,
                                             Long processoId,
                                             String tipo,
                                             boolean ativa,
                                             Instant proximoComparecimento) {}
