package com.tcc.pjb.backend.model.dto.processual.peticionamento.rascunho;

import java.time.Instant;

public record RascunhoConteudoResponse(
        Long draftId,
        String status,
        String tituloCaso,
        String minutaHtml,
        String hashIntegridade,
        int versaoAtual,
        boolean alterado,
        Instant updatedAt
) {
}
