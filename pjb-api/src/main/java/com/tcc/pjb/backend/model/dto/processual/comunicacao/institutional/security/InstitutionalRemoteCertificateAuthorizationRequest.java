package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security;

import java.util.List;

public record InstitutionalRemoteCertificateAuthorizationRequest(
        String affiliationId,
        Long nominatedUserId,
        String reason,
        List<String> allowedNetworks,
        List<String> allowedDevices,
        Integer validForHours,
        List<String> fundamentos
) {
}
