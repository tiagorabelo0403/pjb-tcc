package com.tcc.pjb.backend.service.offline.domain;

public record OfflineBundleOwnershipQuery(
        String bundleToken,
        Long solicitanteId
) {}
