package com.tcc.pjb.backend.model.dto.consultapublica;

import java.time.LocalDateTime;

public record ConsultaPublicaPersonalDeadlineDto(
        String titulo,
        LocalDateTime dataFim,
        boolean vencido,
        long horasRestantes
) {
}
