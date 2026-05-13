package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel;

import java.util.List;

public record NationalCommunicationInstitutionalTriageSuggestionResponse(
        String suggestionId,
        String expedicaoUuid,
        String unidadeCodigo,
        String caixaCodigoOrigem,
        String caixaCodigoSugerida,
        Long usuarioIdSugerido,
        String tipoSugestao,
        int score,
        List<String> fundamentos
) {
}
