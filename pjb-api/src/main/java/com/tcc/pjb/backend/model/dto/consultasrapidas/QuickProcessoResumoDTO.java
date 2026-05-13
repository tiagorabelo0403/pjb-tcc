package com.tcc.pjb.backend.model.dto.consultasrapidas;

import java.time.LocalDateTime;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;

public record QuickProcessoResumoDTO(
        Long processoId,
        String numero,
        String classe,
        String assunto,
        NivelSigilo nivelSigilo,
        boolean sigiloso,
        LocalDateTime dataUltimaMovimentacao
) {
}
