package com.tcc.pjb.backend.model.dto.publico;

import java.time.LocalDateTime;
import java.util.UUID;

public record PublicDocumentoDTO(
        UUID documentoId,
        String titulo,
        String nomeOriginal,
        String contentType,
        Long tamanhoBytes,
        int numeroPaginas,
        LocalDateTime criadoEm
) {
}
