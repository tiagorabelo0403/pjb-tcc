package com.tcc.pjb.backend.service.offline.domain;

public record OfflineReplayResult(String bundleToken, boolean replaySafe, boolean requiresReview, String summary) {}
