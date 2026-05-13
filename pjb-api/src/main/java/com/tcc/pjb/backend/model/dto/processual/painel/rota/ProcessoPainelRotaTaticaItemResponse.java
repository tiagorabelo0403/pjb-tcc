package com.tcc.pjb.backend.model.dto.processual.painel.rota;

public record ProcessoPainelRotaTaticaItemResponse(
        String code,
        String severity,
        String fundamento,
        String acao,
        String navigationPath
) {
}
