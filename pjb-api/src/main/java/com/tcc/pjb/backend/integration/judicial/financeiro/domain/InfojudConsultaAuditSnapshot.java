package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

import java.time.Instant;

public record InfojudConsultaAuditSnapshot(Long id,
                                           Long processoId,
                                           String cpfCnpjConsultado,
                                           String status,
                                           Instant confirmadoEm) {}
