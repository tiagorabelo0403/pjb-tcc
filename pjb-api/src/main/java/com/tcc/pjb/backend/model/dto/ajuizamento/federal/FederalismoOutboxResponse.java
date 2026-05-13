package com.tcc.pjb.backend.model.dto.ajuizamento.federal;

import java.time.Instant;
import java.util.UUID;
import com.tcc.pjb.backend.model.entity.federalismo.FederacaoEventoOutbox;
import com.tcc.pjb.backend.model.entity.federalismo.StatusEventoOutboxFederacao;

public record FederalismoOutboxResponse(
        UUID id,
        String tribunalCodigo,
        String topicKafka,
        String eventType,
        String payloadHash,
        String idempotencyKey,
        String correlationId,
        long schemaVersion,
        int tentativas,
        int prioridade,
        Instant proximaTentativaEm,
        Instant publicadoEm,
        StatusEventoOutboxFederacao status,
        String ultimoErro
) {
    public static FederalismoOutboxResponse of(FederacaoEventoOutbox event) {
        return new FederalismoOutboxResponse(
                event.getId(),
                event.getTribunalCodigo(),
                event.getTopicKafka(),
                event.getEventType(),
                event.getPayloadHash(),
                event.getIdempotencyKey(),
                event.getCorrelationId(),
                event.getSchemaVersion(),
                event.getTentativas(),
                event.getPrioridade(),
                event.getProximaTentativaEm(),
                event.getPublicadoEm(),
                event.getStatus(),
                event.getUltimoErro()
        );
    }
}
