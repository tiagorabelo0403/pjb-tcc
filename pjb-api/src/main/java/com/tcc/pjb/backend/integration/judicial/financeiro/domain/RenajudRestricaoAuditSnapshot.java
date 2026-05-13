package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

import java.time.Instant;

public record RenajudRestricaoAuditSnapshot(Long id,
                                            Long processoId,
                                            String placa,
                                            String status,
                                            Instant confirmadoEm) {}
