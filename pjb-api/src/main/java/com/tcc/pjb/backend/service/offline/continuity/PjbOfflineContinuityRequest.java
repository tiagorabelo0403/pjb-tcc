package com.tcc.pjb.backend.service.offline.continuity;

import java.time.Instant;
import java.util.Set;

public record PjbOfflineContinuityRequest(String processNumber,
                                          String deviceFingerprint,
                                          Instant capturedAt,
                                          Set<PjbOfflineContinuityActionKind> requestedActions,
                                          boolean sealedLocalVault,
                                          boolean latestSnapshotAvailable,
                                          boolean hasSensitiveSecrecy,
                                          boolean conflictDetected) {
}
