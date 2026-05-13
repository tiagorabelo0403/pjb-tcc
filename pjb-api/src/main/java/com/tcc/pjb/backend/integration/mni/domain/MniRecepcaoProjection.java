package com.tcc.pjb.backend.integration.mni.domain;

import java.time.Instant;

public record MniRecepcaoProjection(Long recepcaoId,
                                    String tribunalOrigem,
                                    String motivo,
                                    String status,
                                    Instant receivedAt) {
}
