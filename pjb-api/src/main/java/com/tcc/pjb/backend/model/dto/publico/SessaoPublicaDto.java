package com.tcc.pjb.backend.model.dto.publico;

import java.time.LocalDateTime;

public record SessaoPublicaDto(
        Long sessaoId,
        Long processoId,
        String numeroProcesso,
        String tribunal,
        String orgaoJulgador,
        String relator,
        String status,
        LocalDateTime pautaDataHora,
        LocalDateTime sessaoInicio,
        LocalDateTime sessaoFim,
        Boolean acordaoPublicado,
        String streamUrl
) {
}
