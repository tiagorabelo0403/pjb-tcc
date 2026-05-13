package com.tcc.pjb.backend.service.secretariat.query.queue;

import java.time.Instant;
import java.util.List;
import java.util.Map;


record SecretariatQueuePanelRow(
      String inboxKey,
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
      List<String> tags,
      Map<String, Object> metadata,
      boolean overdue,
      boolean urgent,
      boolean hearingSensitive,
      boolean secrecyReviewRequired,
      String processoKey,
      String ritoKey,
      String varaKey,
      String dataKey,
      String cellCode,
      String secretariatLabel,
      String foroLabel,
      String organLabel,
      Long responsibleUserId,
      String responsibleName,
      String categoryLabel,
      String eventTrack,
      String confirmationStatus,
      String venueConfirmationStatus,
      String participantNotificationStatus,
      String completionEventStatus,
      String attendanceStatus,
      String processReturnStatus,
      String processReturnRoute,
      boolean autoReturnReady,
      int urgencyScore,
      String urgencyExplanation
  ) {
    String processoLabel() {
      return processoNumero;
    }

    String ritoLabel() {
      return ritoProcessual;
    }

    String dataLabel() {
      return dataKey;
    }

    String cellKey() {
      return firstNonBlank(cellCode, "CELULA_BASE");
    }

    String cellLabel() {
      return cellKey();
    }

    String responsibleKey() {
      return firstNonBlank(responsibleName, responsibleUserId == null ? null : String.valueOf(responsibleUserId), "NAO_ATRIBUIDO");
    }

    String responsibleLabel() {
      return responsibleKey();
    }

    String categoryKey() {
      return firstNonBlank(categoryLabel, "OPERACIONAL_GERAL");
    }

    String trackKey() {
      return firstNonBlank(eventTrack, "OPERACIONAL_GERAL");
    }

    String trackLabel() {
      return trackKey();
    }

    String confirmationKey() {
      return firstNonBlank(confirmationStatus, "NAO_APLICAVEL");
    }

    String confirmationLabel() {
      return confirmationKey();
    }

    String venueConfirmationKey() {
      return firstNonBlank(venueConfirmationStatus, "NAO_APLICAVEL");
    }

    String venueConfirmationLabel() {
      return venueConfirmationKey();
    }

    String participantNotificationKey() {
      return firstNonBlank(participantNotificationStatus, "NAO_APLICAVEL");
    }

    String participantNotificationLabel() {
      return participantNotificationKey();
    }

    String attendanceKey() {
      return firstNonBlank(attendanceStatus, "NAO_APLICAVEL");
    }

    String attendanceLabel() {
      return attendanceKey();
    }

    String returnKey() {
      return firstNonBlank(processReturnStatus, "NAO_APLICAVEL");
    }

    String returnLabel() {
      return returnKey();
    }
  

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
