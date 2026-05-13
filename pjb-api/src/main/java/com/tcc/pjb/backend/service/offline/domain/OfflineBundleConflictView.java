package com.tcc.pjb.backend.service.offline.domain;

public record OfflineBundleConflictView(Long bundleId, String status, String conflitoResumo, boolean pendenteRevisao) {}
