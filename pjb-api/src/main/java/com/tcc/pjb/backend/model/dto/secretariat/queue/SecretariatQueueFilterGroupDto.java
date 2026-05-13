package com.tcc.pjb.backend.model.dto.secretariat.queue;

import java.util.List;

public record SecretariatQueueFilterGroupDto(
    String filterCode,
    String filterLabel,
    List<String> values
) {
}
