package com.tcc.pjb.backend.core.security.domain;

import java.time.Instant;

public record GovBrAssuranceAuditSnapshot(String nivelAtual, boolean atoSensivel, boolean atendido, Instant avaliadoEm) {}
