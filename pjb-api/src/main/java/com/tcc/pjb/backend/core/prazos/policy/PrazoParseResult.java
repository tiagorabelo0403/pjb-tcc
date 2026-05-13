package com.tcc.pjb.backend.core.prazos.policy;

import com.tcc.pjb.backend.core.prazos.PrazoRegime;


public record PrazoParseResult(
        Integer dias,
        PrazoRegime regime,
        String matchedSnippet
) {}
