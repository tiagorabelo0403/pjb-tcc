package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance;

import java.time.Instant;

public record NationalCommunicationInstitutionalSlaPredictiveAlertResponse(
        String unidadeCodigo,
        String unidadeSigla,
        String destinatarioKind,
        String uf,
        long pendenciasCiencia,
        long pendenciasCumprimento,
        double mediaHistoricaHorasResposta,
        long horasRestantesMinimas,
        String risco,
        String mensagem,
        Instant generatedAt
) {
}
