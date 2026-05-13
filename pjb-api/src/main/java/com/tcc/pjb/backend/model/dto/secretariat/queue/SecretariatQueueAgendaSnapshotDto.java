package com.tcc.pjb.backend.model.dto.secretariat.queue;

import java.util.List;
import java.util.Map;

public record SecretariatQueueAgendaSnapshotDto(
    String inboxKey,
    String inboxDescriptor,
    String deskDescriptor,
    List<SecretariatQueueAgendaGroupDto> byData,
    List<SecretariatQueueAgendaGroupDto> byVara,
    List<SecretariatQueueAgendaGroupDto> byRito,
    List<SecretariatQueueAgendaGroupDto> byCell,
    List<SecretariatQueueAgendaGroupDto> byResponsible,
    List<SecretariatQueueAgendaGroupDto> byCategory,
    List<SecretariatQueueAgendaGroupDto> byTrack,
    List<SecretariatQueueAgendaGroupDto> byConfirmation,
    List<SecretariatQueueAgendaGroupDto> byVenueConfirmation,
    List<SecretariatQueueAgendaGroupDto> byParticipantNotification,
    List<SecretariatQueueAgendaGroupDto> byAttendance,
    List<SecretariatQueueAgendaGroupDto> byReturnStatus,
    List<SecretariatQueueDeadlineBucketDto> deadlineBuckets,
    List<SecretariatQueueFilterGroupDto> filters,
    List<SecretariatQueueAgendaItemDto> items,
    Map<String, Object> metadata
) {
}
