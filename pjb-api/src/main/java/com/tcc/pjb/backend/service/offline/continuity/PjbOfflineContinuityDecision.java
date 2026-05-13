package com.tcc.pjb.backend.service.offline.continuity;

import java.util.List;

public record PjbOfflineContinuityDecision(String status,
                                           boolean offlineAllowed,
                                           boolean replayRequiresHumanReview,
                                           List<String> allowedActions,
                                           List<String> blockers) {
}
