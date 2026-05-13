package com.tcc.pjb.backend.model.dto.processual.linkage;

import java.util.List;
import com.tcc.pjb.backend.model.entity.enums.VinculoProcessualTipo;

public record ProcessoLinkageCandidateResponse(
        Long processoId,
        String numeroProcesso,
        VinculoProcessualTipo vinculoTipo,
        int score,
        boolean recomendado,
        String preventionMode,
        String linkageMode,
        List<String> fundamentos) {
}
