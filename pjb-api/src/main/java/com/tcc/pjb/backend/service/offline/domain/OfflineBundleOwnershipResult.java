package com.tcc.pjb.backend.service.offline.domain;

public record OfflineBundleOwnershipResult(
        String bundleToken,
        Long solicitanteId,
        boolean owner,
        String scope
) {}
