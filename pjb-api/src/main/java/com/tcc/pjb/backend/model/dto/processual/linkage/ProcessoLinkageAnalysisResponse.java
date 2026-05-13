package com.tcc.pjb.backend.model.dto.processual.linkage;

import java.util.List;
import com.tcc.pjb.backend.model.entity.enums.VinculoProcessualTipo;

public record ProcessoLinkageAnalysisResponse(
        Long processoId,
        String numeroProcesso,
        int totalAnalisado,
        VinculoProcessualTipo recomendacaoPrimaria,
        String preventionModeSugerido,
        String linkageModeSugerido,
        List<String> alertas,
        List<ProcessoLinkageCandidateResponse> candidatos) {
}
