package com.tcc.pjb.backend.core.security.domain;

import java.time.Instant;

public record GovBrAssurancePolicyView(String nivelMinimoNormal, String nivelMinimoSensivel, Instant geradoEm) {}
