package com.tcc.pjb.backend.core.criminal.custodia.domain;

import java.time.Instant;

public record CustodiaTimelineEntry(String evento, Instant em, String detalhe) {}
