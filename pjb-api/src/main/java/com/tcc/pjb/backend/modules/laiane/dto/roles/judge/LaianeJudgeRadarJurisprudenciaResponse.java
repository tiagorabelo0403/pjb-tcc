package com.tcc.pjb.backend.modules.laiane.dto.roles.judge;

import java.time.Instant;
import java.util.List;

public record LaianeJudgeRadarJurisprudenciaResponse(
        Long processoId,
        String numeroProcesso,
        String rito,
        String ramoDireito,
        String status,
        double aderenciaContextual,
        List<String> consultasSugeridas,
        List<String> cautelas,
        List<String> eixosAnalise,
        List<LaianeJudgeRadarHitDto> hits,
        Instant generatedAt
) {
}
