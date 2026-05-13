package com.tcc.pjb.backend.model.dto.secretariat.queue;

import java.util.List;

public record SecretariatQueueDeadlineBucketDto(
    String bucketCode,
    String bucketLabel,
    long itemCount,
    long processCount,
    List<Long> workItemIds
) {
}
