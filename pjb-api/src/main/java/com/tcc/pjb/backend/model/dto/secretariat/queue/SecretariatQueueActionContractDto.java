package com.tcc.pjb.backend.model.dto.secretariat.queue;

import java.util.List;

public record SecretariatQueueActionContractDto(
    String actionCode,
    String label,
    String method,
    String route,
    String payloadType,
    boolean enabled,
    String blockingReason,
    String targetPanelRoute,
    List<String> refreshRoutes
) {
}
