package com.tcc.pjb.backend.core.security.domain;

import java.time.Instant;

public record GovBrAssuranceTimelineEntry(String fase, String nivelAtual, boolean permitido, Instant instante) {}
