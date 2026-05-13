package com.tcc.pjb.backend.service.offline.domain;

import java.time.Instant;

public record OfflineBundleSyncSnapshot(String status, Instant sincronizadoEm, String conflitoResumo) {}
