package com.tcc.pjb.backend.model.dto.transito;

import java.time.LocalDateTime;

public record ArquivamentoCandidatoResponse(
        Long processoId,
        String numeroProcesso,
        String classeProcessual,
        LocalDateTime dataUltimaMovimentacao
) {}
