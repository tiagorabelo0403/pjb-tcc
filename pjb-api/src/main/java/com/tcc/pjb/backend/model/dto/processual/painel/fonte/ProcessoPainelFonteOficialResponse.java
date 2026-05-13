package com.tcc.pjb.backend.model.dto.processual.painel.fonte;

import java.time.Instant;
import java.util.List;

public record ProcessoPainelFonteOficialResponse(
        Long processoId,
        String numeroProcesso,
        String ramoDireito,
        List<ProcessoPainelFonteOficialItemResponse> itens,
        List<String> garantias,
        Instant geradoEm
) {
    public ProcessoPainelFonteOficialResponse {
        itens = itens == null ? List.of() : List.copyOf(itens);
        garantias = garantias == null ? List.of() : List.copyOf(garantias);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
