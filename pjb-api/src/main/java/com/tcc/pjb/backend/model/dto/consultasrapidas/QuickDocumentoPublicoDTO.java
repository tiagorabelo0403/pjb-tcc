package com.tcc.pjb.backend.model.dto.consultasrapidas;

import java.time.LocalDateTime;

public record QuickDocumentoPublicoDTO(
        String id,
        String titulo,
        String nomeOriginal,
        String contentType,
        Long tamanhoBytes,
        Integer numeroPaginas,
        LocalDateTime criadoEm,
        String downloadUrl
) {
}
