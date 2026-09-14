package com.tcc.pjb.backend.model.dto.mni;

import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import java.util.UUID;

public record DocumentoClassificadoResponse(UUID id, String tipoDocumento) {

    public static DocumentoClassificadoResponse from(DocumentoProcessual documento) {
        return new DocumentoClassificadoResponse(documento.getId(),
                documento.getTipoDocumento() == null ? null : documento.getTipoDocumento().name());
    }
}
