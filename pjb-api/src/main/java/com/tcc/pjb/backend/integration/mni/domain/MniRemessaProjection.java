package com.tcc.pjb.backend.integration.mni.domain;

import java.time.Instant;

public record MniRemessaProjection(Long remessaId,
                                   Long processoId,
                                   String tribunalDestino,
                                   String motivo,
                                   String status,
                                   Instant createdAt) {
}
