package com.tcc.pjb.backend.service.offline.domain;

public record OfflineBundleOwnershipView(
        Long bundleId,
        Long processoId,
        Long solicitanteId,
        String deviceFingerprint
) {}
