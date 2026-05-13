package com.tcc.pjb.backend.core.dje.domain;

import java.util.List;

public record DjeTimelineView(Long djeId,
                              List<DjeTimelineEntry> entries) {}
