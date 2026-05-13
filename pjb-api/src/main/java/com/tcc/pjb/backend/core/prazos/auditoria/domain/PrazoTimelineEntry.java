package com.tcc.pjb.backend.core.prazos.auditoria.domain;

import java.time.Instant;

public record PrazoTimelineEntry(String stage, Instant at, String detail) {}
