package com.tcc.pjb.backend.model.dto.secretariat.queue;

import java.time.Instant;
import java.util.List;

public record SecretariatQueueParticipantNotificationRequest(
    String status,
    Integer readyCount,
    Integer pendingCount,
    Integer missingCount,
    String channel,
    Instant notifiedAt,
    String note,
    Long challengeId,
    String otpCode,
    String formaIntimacao,
    String provaResumo,
    List<String> evidenceReferences
) {
    public SecretariatQueueParticipantNotificationRequest(String status,
                                                          Integer readyCount,
                                                          Integer pendingCount,
                                                          Integer missingCount,
                                                          String channel,
                                                          Instant notifiedAt,
                                                          String note) {
        this(status, readyCount, pendingCount, missingCount, channel, notifiedAt, note, null, null, null, null, List.of());
    }
}
