package com.tcc.pjb.backend.model.dto.consultapublica;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConsultaPublicaPersonalProcessTagDto(
        UUID id,
        String nome,
        String corHex,
        boolean sistema,
        LocalDateTime atualizadoEm
) {
}
