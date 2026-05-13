package com.tcc.pjb.backend.model.dto.oficial_justica;

import java.time.Instant;

public record OficialJusticaNotificationEnvelope(
        Long usuarioId,
        Long processoId,
        Long workItemId,
        String notificationKey,
        String notificationType,
        String title,
        String body,
        String detailsUrl,
        boolean highPriority,
        String processNumber,
        String territorialWindow,
        String complianceType,
        String originLabel,
        Instant generatedAt
) {
}
