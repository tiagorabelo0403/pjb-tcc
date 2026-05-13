package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

import java.time.Instant;

public record IntegracaoJudicialTimelineEntry(String integracao, String status, Instant quando, String detalhe) {}
