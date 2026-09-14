package com.tcc.pjb.backend.model.dto.mni;

import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import jakarta.validation.constraints.NotNull;

public record ConfirmarClassificacaoDocumentoRequest(@NotNull TipoDocumento tipoDocumento) {
}
