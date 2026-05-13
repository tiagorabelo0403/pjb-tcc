package com.tcc.pjb.backend.core.criminal.custodia.domain;

import java.time.Instant;

public record CustodiaAndamentoSnapshot(Long custodiaId,
                                        String status,
                                        Instant prazoLimite24h,
                                        Instant realizadaEm) {
}
