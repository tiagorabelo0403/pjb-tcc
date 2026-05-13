package com.tcc.pjb.backend.service.offline.domain;

import jakarta.validation.constraints.NotNull;

public record CriarBundleRequest(@NotNull Long processoId, String escopo, String deviceFingerprint) {
}
