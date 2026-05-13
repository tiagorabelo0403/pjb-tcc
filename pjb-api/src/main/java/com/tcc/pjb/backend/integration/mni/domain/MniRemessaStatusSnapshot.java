package com.tcc.pjb.backend.integration.mni.domain;

import java.time.Instant;

public record MniRemessaStatusSnapshot(Long remessaId,
                                       String status,
                                       String protocoloDestino,
                                       Instant sentAt,
                                       Instant confirmedAt) {}
