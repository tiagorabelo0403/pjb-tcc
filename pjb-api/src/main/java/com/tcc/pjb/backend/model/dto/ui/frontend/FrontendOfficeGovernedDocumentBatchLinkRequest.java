package com.tcc.pjb.backend.model.dto.ui.frontend;

import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record FrontendOfficeGovernedDocumentBatchLinkRequest(
        @NotNull UUID batchId,
        String titulo,
        DocumentoCategoria categoria,
        NivelSigilo nivelSigilo,
        String origemSistema
) {
}
