package com.tcc.pjb.backend.model.dto.secretariat.queue;

import java.util.List;
import java.util.Map;

public record SecretariatQueuePanelSnapshotDto(
    String inboxKey,
    String inboxDescriptor,
    String deskDescriptor,
    String dashboardBucket,
    List<SecretariatQueuePanelGroupDto> byProcesso,
    List<SecretariatQueuePanelGroupDto> byRito,
    List<SecretariatQueuePanelGroupDto> byVara,
    List<SecretariatQueuePanelGroupDto> byData,
    Map<String, Object> metadata
) {
}
