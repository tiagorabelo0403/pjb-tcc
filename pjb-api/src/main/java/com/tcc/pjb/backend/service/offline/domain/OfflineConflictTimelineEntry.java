package com.tcc.pjb.backend.service.offline.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record OfflineConflictTimelineEntry(@JsonProperty("evento") String stage, Instant at, String detail) {}
