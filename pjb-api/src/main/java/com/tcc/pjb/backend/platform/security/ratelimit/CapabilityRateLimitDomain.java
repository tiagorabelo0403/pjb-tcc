package com.tcc.pjb.backend.platform.security.ratelimit;

import java.util.Locale;

public enum CapabilityRateLimitDomain {
    JURIDICA,
    FINANCEIRA,
    LEGAL_SKILLS,
    CITIZEN,
    LAWYER,
    INSTITUCIONAL,
    SERVIDOR;

    public String canonical() {
        return name().toLowerCase(Locale.ROOT);
    }
}
