package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalOfficialSourceConnectorCatalogResponse(
        Instant generatedAt,
        List<NationalCommunicationInstitutionalOfficialSourceConnectorResponse> sources
) {
}
