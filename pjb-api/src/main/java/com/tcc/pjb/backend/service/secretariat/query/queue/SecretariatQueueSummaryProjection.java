package com.tcc.pjb.backend.service.secretariat.query.queue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SecretariatQueueSummaryProjection(
    Map<String, Object> metadata,
    List<String> labels
) {

    public SecretariatQueueSummaryProjection {
        metadata = metadata == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(metadata));
        labels = labels == null ? List.of() : List.copyOf(labels);
    }
}
