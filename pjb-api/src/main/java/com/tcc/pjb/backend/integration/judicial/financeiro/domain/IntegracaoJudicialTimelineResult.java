package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

import java.util.List;

public record IntegracaoJudicialTimelineResult(String integracao, Long id, List<IntegracaoJudicialTimelineEntry> entries) {}
