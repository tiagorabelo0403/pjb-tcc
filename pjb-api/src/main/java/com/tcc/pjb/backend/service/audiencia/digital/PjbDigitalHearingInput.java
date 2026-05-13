package com.tcc.pjb.backend.service.audiencia.digital;

import java.time.Instant;
import java.util.Set;

public record PjbDigitalHearingInput(String processNumber,
                                     Instant scheduledAt,
                                     Set<String> requiredProfiles,
                                     boolean videoRoomProvisioned,
                                     boolean recordingProvisioned,
                                     boolean transcriptionProvisioned,
                                     boolean accessibilityRequested,
                                     boolean accessibilityProvisioned,
                                     boolean identityCheckRequired,
                                     boolean identityCheckProvisioned) {
}
