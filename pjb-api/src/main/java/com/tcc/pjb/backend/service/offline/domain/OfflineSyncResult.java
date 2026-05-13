package com.tcc.pjb.backend.service.offline.domain;

public record OfflineSyncResult(String bundleToken, String status, String conflictSummary) {}
