package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

import java.time.Instant;

public record RenajudRestricaoSnapshot(Long restricaoId,
                                       String placa,
                                       String status,
                                       Instant confirmadoEm) {
}
