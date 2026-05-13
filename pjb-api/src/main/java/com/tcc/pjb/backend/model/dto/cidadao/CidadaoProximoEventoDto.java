package com.tcc.pjb.backend.model.dto.cidadao;

import java.time.LocalDateTime;
import java.util.List;

public record CidadaoProximoEventoDto(
        String type,
        LocalDateTime quando,
        Long processoId,
        String numeroUnificado,
        String title,
        String detalhe,
        List<String> uiTokens,
        Links links
) {
}
