package com.tcc.pjb.backend.modules.laiane.dto.roles.judge;

import java.time.LocalDate;
import java.util.List;

public record LaianeJudgeRadarHitDto(
        Long precedenteId,
        String fonte,
        String tipo,
        String identificador,
        String titulo,
        String urlReferencia,
        LocalDate dataPublicacao,
        double aderenciaEstimada,
        List<String> citacoesDetectadas
) {
}
