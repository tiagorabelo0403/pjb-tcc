package com.tcc.pjb.backend.model.dto.secretariat.queue;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SecretariatQueueAgendaItemDto(
    Long workItemId,
    Long processoId,
    String processoNumero,
    String titulo,
    String queueCode,
    String stage,
    String status,
    Integer prioridade,
    Instant referenceAt,
    Instant dueAt,
    String ritoProcessual,
    String classeProcessual,
    String varaLabel,
    String unidadeCodigo,
    String comarca,
    String tribunalCodigo,
    String foroLabel,
    String secretariatLabel,
    String cellCode,
    Long responsibleUserId,
    String responsibleName,
    String category,
    String eventTrack,
    String confirmationStatus,
    String attendanceStatus,
    String processReturnStatus,
    String processReturnRoute,
    boolean autoReturnReady,
    String venueConfirmationStatus,
    String participantNotificationStatus,
    String completionEventStatus,
    String targetPanelRoute,
    SecretariatQueueAgendaVenueDto venue,
    SecretariatQueueAgendaNotificationSummaryDto notification,
    SecretariatQueueAgendaCompletionDto completion,
    List<SecretariatQueueAgendaChecklistItemDto> checklist,
    List<SecretariatQueueAgendaContactDto> contacts,
    List<SecretariatQueueActionContractDto> actionContracts,
    Map<String, Object> metadata
) {
}
