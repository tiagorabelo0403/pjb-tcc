package com.tcc.pjb.backend.core.protocolo.completude.domain;

import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;

public record DocumentoAnalisavel(
        TipoDocumento tipo,
        byte[] pdf,
        NivelSigilo nivelSigilo,
        String storageUri,
        String sha256
) {
}
