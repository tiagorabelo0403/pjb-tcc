package com.tcc.pjb.backend.core.security.domain;

import java.time.Instant;

public record GovBrAssuranceHealthResult(boolean configured, boolean aptoParaAtoSensivel, boolean aptoParaAtoNormal, Instant checkedAt) {}
