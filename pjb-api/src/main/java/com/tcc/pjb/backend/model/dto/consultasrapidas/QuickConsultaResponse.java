package com.tcc.pjb.backend.model.dto.consultasrapidas;

import java.util.List;

public record QuickConsultaResponse(
        QuickProcessoResumoDTO processo,
        String mensagem,
        List<QuickDocumentoPublicoDTO> documentosPublicos
) {
}
