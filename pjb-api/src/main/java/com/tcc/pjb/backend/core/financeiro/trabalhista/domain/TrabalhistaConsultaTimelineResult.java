package com.tcc.pjb.backend.core.financeiro.trabalhista.domain;

import java.util.List;

public record TrabalhistaConsultaTimelineResult(Long processoId, List<TrabalhistaTimelineEntry> entries) {}
