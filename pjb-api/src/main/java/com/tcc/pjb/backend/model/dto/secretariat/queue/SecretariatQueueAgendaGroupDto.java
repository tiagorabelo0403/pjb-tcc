package com.tcc.pjb.backend.model.dto.secretariat.queue;

import java.time.Instant;
import java.util.List;

public record SecretariatQueueAgendaGroupDto(
    String axis,
    String groupKey,
    String groupLabel,
    Instant referenceAt,
    long itemCount,
    long processCount,
    long contactReadyCount,
    List<SecretariatQueueAgendaItemDto> items
) {
}
