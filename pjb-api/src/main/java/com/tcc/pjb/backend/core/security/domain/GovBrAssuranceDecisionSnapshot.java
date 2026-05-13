package com.tcc.pjb.backend.core.security.domain;

import java.time.Instant;

public record GovBrAssuranceDecisionSnapshot(String nivelAtual, boolean atoSensivel, boolean permitido, boolean exigeStepUp, Instant decidedAt) {}
