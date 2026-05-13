package com.tcc.pjb.backend.core.dje.domain;

import java.time.Instant;

public record DjeTimelineEntry(String evento,
                               Instant em,
                               String detalhe) {
}
