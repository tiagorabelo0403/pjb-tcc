package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology;

import java.util.List;

public record NationalCommunicationInstitutionalOperatingCoverageResponse(
        String requestedMunicipality,
        String requestedUf,
        String requestedKind,
        boolean localUnitPresent,
        String responsibleUnitCode,
        String responsibleUnitName,
        String responsibleForo,
        String responsibleComarca,
        String responsibleTribunalCode,
        String coverageMode,
        List<String> fallbackChain,
        List<String> fundamentos
) {
}
