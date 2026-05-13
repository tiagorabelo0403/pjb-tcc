package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain;

import java.time.Instant;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;

public record InstitutionalSlaPredictiveAlert(
        String unidadeCodigo,
        String unidadeSigla,
        DestinatarioInstitucionalKind destinatarioKind,
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
