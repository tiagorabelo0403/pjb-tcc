package com.tcc.pjb.backend.model.entity.enums;

import java.util.Map;
import com.tcc.pjb.backend.core.util.EnumText;

public enum TribunalFonte {
    STF,
    STJ,
    TST,
    TSE,
    STM,
    CNJ,
    TRF,
    TJ,
    TRT,
    TRE,
    OUTRO;

    
    public static TribunalFonte fromString(String raw) {
        if (raw == null || raw.isBlank()) return OUTRO;
        String token = EnumText.normalizeToken(raw);
        if (token.isBlank()) return OUTRO;

        TribunalFonte alias = ALIASES.get(token);
        if (alias != null) return alias;

        try {
            return TribunalFonte.valueOf(token);
        } catch (Exception ignored) {
            return OUTRO;
        }
    }

    private static final Map<String, TribunalFonte> ALIASES = Map.ofEntries(
            Map.entry("SUPREMO", STF),
            Map.entry("SUPREMO_TRIBUNAL_FEDERAL", STF),
            Map.entry("STF", STF),

            Map.entry("SUPERIOR_TRIBUNAL_DE_JUSTICA", STJ),
            Map.entry("STJ", STJ),

            Map.entry("TRIBUNAL_SUPERIOR_DO_TRABALHO", TST),
            Map.entry("TST", TST),

            Map.entry("TRIBUNAL_SUPERIOR_ELEITORAL", TSE),
            Map.entry("TSE", TSE),

            Map.entry("SUPERIOR_TRIBUNAL_MILITAR", STM),
            Map.entry("STM", STM),

            Map.entry("CONSELHO_NACIONAL_DE_JUSTICA", CNJ),
            Map.entry("CNJ", CNJ),

            Map.entry("TRIBUNAL_REGIONAL_FEDERAL", TRF),
            Map.entry("TRF", TRF),

            Map.entry("TRIBUNAL_DE_JUSTICA", TJ),
            Map.entry("TJ", TJ),

            Map.entry("TRIBUNAL_REGIONAL_DO_TRABALHO", TRT),
            Map.entry("TRT", TRT),

            Map.entry("TRIBUNAL_REGIONAL_ELEITORAL", TRE),
            Map.entry("TRE", TRE)
    );
}
