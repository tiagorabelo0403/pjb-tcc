package com.tcc.pjb.backend.core.judicial.sobrestamento.domain;

import java.util.List;

public record SobrestamentoTemaTimelineResult(String codigoTema, List<SobrestamentoTemaTimelineEntry> entries) {}
