package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

import java.time.Instant;

public record RenajudConsultaResult(Long id,
                                    String placa,
                                    String status,
                                    String protocolo,
                                    Instant confirmadoEm) {}
