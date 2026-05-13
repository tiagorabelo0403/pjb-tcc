package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel;

import java.time.Instant;

public record NationalCommunicationInstitutionalUnitQueueResponse(
        String unidadeCodigo,
        String unidadeSigla,
        String caixaCodigo,
        long total,
        long disponibilizadas,
        long recebidas,
        long cientificadas,
        long cumpridas,
        long atrasadas,
        String horizontalDataPlaneKey,
        String rlsScopeKey,
        String coverageMode,
        boolean readOnly,
        Instant prazoMaisProximo,
        Instant generatedAt
) {
}
