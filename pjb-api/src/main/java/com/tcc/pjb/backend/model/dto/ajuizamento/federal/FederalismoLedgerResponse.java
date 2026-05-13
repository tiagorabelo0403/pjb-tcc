package com.tcc.pjb.backend.model.dto.ajuizamento.federal;

import java.time.Instant;
import com.tcc.pjb.backend.model.entity.federalismo.ClassificacaoConflitoFederacao;
import com.tcc.pjb.backend.model.entity.federalismo.FederacaoLedgerEntry;
import com.tcc.pjb.backend.model.entity.federalismo.StatusAssinaturaFederacao;

public record FederalismoLedgerResponse(
        Long id,
        long sequenciaGlobal,
        long sequenciaTribunal,
        String hashEntrada,
        String hashAnterior,
        String tribunalCodigo,
        String tipoEvento,
        String topicKafka,
        String nupn,
        String payloadHash,
        String operadorId,
        long schemaVersion,
        String correlationId,
        String idempotencyKey,
        StatusAssinaturaFederacao statusAssinatura,
        ClassificacaoConflitoFederacao classificacaoConflito,
        int tamanhoPayloadBytes,
        Instant ocorridoEm
) {
    public static FederalismoLedgerResponse of(FederacaoLedgerEntry entry) {
        return new FederalismoLedgerResponse(
                entry.getId(),
                entry.getSequenciaGlobal(),
                entry.getSequenciaTribunal(),
                entry.getHashEntrada(),
                entry.getHashAnterior(),
                entry.getTribunalCodigo(),
                entry.getTipoEvento(),
                entry.getTopicKafka(),
                entry.getNupn(),
                entry.getPayloadHash(),
                entry.getOperadorId(),
                entry.getSchemaVersion(),
                entry.getCorrelationId(),
                entry.getIdempotencyKey(),
                entry.getStatusAssinatura(),
                entry.getClassificacaoConflito(),
                entry.getTamanhoPayloadBytes(),
                entry.getOcorridoEm()
        );
    }
}
