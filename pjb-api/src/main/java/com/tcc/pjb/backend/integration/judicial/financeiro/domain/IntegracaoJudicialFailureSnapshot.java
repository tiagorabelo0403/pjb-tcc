package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

import java.time.Instant;

public record IntegracaoJudicialFailureSnapshot(String integracao, Long id, String status, Integer tentativas, Instant proximoRetryEm) {}
