package com.tcc.pjb.backend.core.dje.domain;
import java.util.List;
public record DjeConsultaTimelineResult(Long djeId, List<DjeTimelineEntry> entries) {}
