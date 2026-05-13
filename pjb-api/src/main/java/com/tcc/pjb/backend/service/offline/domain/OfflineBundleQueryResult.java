package com.tcc.pjb.backend.service.offline.domain;

import java.time.Instant;

public record OfflineBundleQueryResult(Long bundleId, String bundleToken, Long processoId, String escopo, String status, Instant abertoEm, Instant sincronizadoEm, Instant expiraEm, String conflitoResumo) {}
