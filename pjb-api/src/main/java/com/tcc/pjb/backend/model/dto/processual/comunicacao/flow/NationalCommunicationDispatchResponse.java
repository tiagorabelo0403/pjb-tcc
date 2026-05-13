package com.tcc.pjb.backend.model.dto.processual.comunicacao.flow;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationDispatchResponse(
        String expedicaoUuid,
        Long processoId,
        String tipoComunicacao,
        String modalidade,
        String status,
        String destinatarioDocumento,
        String destinatarioNome,
        String canalDigitalUtilizado,
        Instant expedidaEm,
        Instant presuncaoEntregaEm,
        List<String> alertas,
        List<String> cascataModalidades,
        String hashIntegridade,
        String fundamentacaoLegal,
        boolean antiEvasaoAtivado,
        Long workItemId) {
}
