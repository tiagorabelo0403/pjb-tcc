package com.tcc.pjb.backend.service.offline.domain;

import java.time.Instant;
import java.util.Map;

public record BundleView(Long id,
                         String bundleToken,
                         Long processoId,
                         String numeroProcesso,
                         String status,
                         String escopo,
                         String deviceFingerprint,
                         String manifestHash,
                         Instant abertoEm,
                         Instant expiraEm,
                         Instant sincronizadoEm,
                         String conflitoResumo,
                         Map<String, Object> manifest) {
}
