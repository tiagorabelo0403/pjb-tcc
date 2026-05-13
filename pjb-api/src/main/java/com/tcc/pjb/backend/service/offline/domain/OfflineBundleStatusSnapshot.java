package com.tcc.pjb.backend.service.offline.domain;

import java.time.Instant;

public record OfflineBundleStatusSnapshot(Long bundleId, String status, Instant abertoEm, Instant sincronizadoEm) {}
