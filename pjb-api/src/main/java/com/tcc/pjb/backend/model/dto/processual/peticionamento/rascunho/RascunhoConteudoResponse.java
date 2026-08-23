package com.tcc.pjb.backend.model.dto.processual.peticionamento.rascunho;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record RascunhoConteudoResponse(
        Long draftId,
        String status,
        String tituloCaso,
        JsonNode conteudoJson,
        String minutaHtml,
        String hashIntegridade,
        int versaoAtual,
        boolean alterado,
        Instant updatedAt
) {
}
