package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel;

import java.time.Instant;
import java.util.Map;

public record NationalCommunicationInstitutionalTimelineEventResponse(
        String eventId,
        String expedicaoUuid,
        Long processoId,
        String processoNumero,
        String eventType,
        String statusComunicacao,
        String unidadeCodigo,
        String caixaCodigo,
        Long actorUserId,
        String actorTipoUsuario,
        String resumo,
        Map<String, Object> detalhes,
        Instant occurredAt,
        String hashIntegridade
) {
}
