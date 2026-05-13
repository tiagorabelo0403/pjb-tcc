package com.tcc.pjb.backend.service.offline.domain;

import java.time.Instant;

public record OfflineBundleTimelineEntry(String etapa, Instant quando, String detalhe) {}
