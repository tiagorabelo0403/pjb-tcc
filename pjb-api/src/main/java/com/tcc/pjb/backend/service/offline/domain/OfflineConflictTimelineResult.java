package com.tcc.pjb.backend.service.offline.domain;

import java.util.List;

public record OfflineConflictTimelineResult(String bundleToken, List<OfflineConflictTimelineEntry> entries) {}
