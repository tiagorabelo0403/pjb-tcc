package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalManagedCredentialResponse(
        String credentialId,
        String affiliationId,
        String nominationId,
        Long nominatedUserId,
        String nominatedUserName,
        String managedUsername,
        String displayName,
        String laneCode,
        boolean signerOrSensitive,
        boolean allowsInstitutionManagedLogin,
        boolean govBrBindingRequired,
        boolean govBrBindingConfirmed,
        String status,
        int rotationWindowDays,
        List<String> allowedNetworks,
        List<String> findings,
        List<String> fundamentos,
        Instant createdAt,
        Instant updatedAt
) {
}
