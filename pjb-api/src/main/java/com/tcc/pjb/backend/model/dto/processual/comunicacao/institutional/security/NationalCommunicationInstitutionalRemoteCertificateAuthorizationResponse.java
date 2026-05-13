package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalRemoteCertificateAuthorizationResponse(
        String authorizationId,
        String affiliationId,
        Long nominatedUserId,
        Long issuedByUserId,
        String issuedByUserName,
        String reason,
        List<String> allowedNetworks,
        List<String> allowedDevices,
        Instant validFrom,
        Instant validUntil,
        String status,
        List<String> fundamentos,
        Instant createdAt,
        Instant updatedAt
) {
}
