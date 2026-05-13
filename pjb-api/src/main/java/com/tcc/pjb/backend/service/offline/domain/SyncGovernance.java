package com.tcc.pjb.backend.service.offline.domain;

import java.util.Map;

public record SyncGovernance(String status, String conflictSummary, Map<String, Object> envelope) {
}
