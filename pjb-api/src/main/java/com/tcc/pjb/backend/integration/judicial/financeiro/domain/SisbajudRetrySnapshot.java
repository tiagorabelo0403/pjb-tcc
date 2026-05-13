package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

import java.time.Instant;

public record SisbajudRetrySnapshot(Long operacaoId,
                                    int tentativas,
                                    Instant proximoRetryEm,
                                    String status) {
}
