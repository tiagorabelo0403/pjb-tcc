package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalTriageSuggestionDashboardResponse(
        String expedicaoUuid,
        String unidadeCodigo,
        String caixaAtual,
        List<NationalCommunicationInstitutionalTriageSuggestionResponse> suggestions,
        List<String> notes,
        Instant generatedAt
) {
}
