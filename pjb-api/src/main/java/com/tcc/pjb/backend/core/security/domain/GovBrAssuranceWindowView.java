package com.tcc.pjb.backend.core.security.domain;

import java.time.Instant;

public record GovBrAssuranceWindowView(String nivelAtual, String nivelRequerido, boolean dentroJanela, Instant avaliadoEm) {}
