package com.tcc.pjb.backend.modules.custas.domain;

import java.time.Instant;

public record CustaTimelineEntry(String evento, Instant ocorridoEm, String detalhe) {}
