package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

import java.time.Instant;

public record InfojudConsultaSnapshot(Long consultaId,
                                      String cpfConsultado,
                                      String status,
                                      Instant confirmadoEm) {
}
