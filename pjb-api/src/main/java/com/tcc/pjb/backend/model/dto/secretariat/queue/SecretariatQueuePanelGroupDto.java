package com.tcc.pjb.backend.model.dto.secretariat.queue;

import java.time.Instant;
import java.util.List;

public record SecretariatQueuePanelGroupDto(
    String axis,
    String groupKey,
    String groupLabel,
    Instant referenceAt,
    long itemCount,
    long processCount,
    long urgentCount,
    List<SecretariatQueuePanelItemDto> items
) {
}
