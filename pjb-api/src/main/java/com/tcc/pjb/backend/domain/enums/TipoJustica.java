package com.tcc.pjb.backend.domain.enums;

import java.util.EnumSet;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.tcc.pjb.backend.core.util.EnumText;

public enum TipoJustica {

    ESTADUAL("1"),
    FEDERAL("4"),
    ELEITORAL("6"),
    MILITAR_ESTADUAL("7"),
    MILITAR_FEDERAL("8"),
    TRABALHO("5"),
    SUPERIOR("9");

    private final String codigoCNJ;

    private static final Map<String, TipoJustica> ALIASES = Map.of(
            "JUSTICA_ESTADUAL", ESTADUAL,
            "JUSTICA_FEDERAL", FEDERAL,
            "JUSTICA_ELEITORAL", ELEITORAL,
            "JUSTICA_TRABALHO", TRABALHO,
            "JUSTICA_DO_TRABALHO", TRABALHO,
            "JUSTICA_MILITAR_ESTADUAL", MILITAR_ESTADUAL,
            "JUSTICA_MILITAR_FEDERAL", MILITAR_FEDERAL,
            "STJ", SUPERIOR,
            "STF", SUPERIOR,
            "SUPERIOR_TRIBUNAL", SUPERIOR
    );

    TipoJustica(String codigoCNJ) {
        this.codigoCNJ = codigoCNJ;
    }

    public String getCodigoCNJ() {
        return codigoCNJ;
    }

    
    public static TipoJustica fromString(String raw) {
        if (raw == null || raw.isBlank()) return null;

        String token = EnumText.normalizeToken(raw);
        if (token.isBlank()) return null;

        
        for (TipoJustica v : values()) {
            if (v.codigoCNJ.equals(token)) {
                return v;
            }
        }

        TipoJustica alias = ALIASES.get(token);
        if (alias != null) return alias;

        try {
            return TipoJustica.valueOf(token);
        } catch (Exception ignored) {
            return null;
        }
    }

    @JsonCreator
    public static TipoJustica jsonCreator(String raw) {
        return fromString(raw);
    }

    

    public boolean admiteSegredoJustica() {
        return EnumSet.of(
                FEDERAL,
                ELEITORAL,
                MILITAR_ESTADUAL,
                MILITAR_FEDERAL
        ).contains(this);
    }

    public boolean exigeAtuacaoMP() {
        return this != TRABALHO;
    }

    public boolean admiteJuizado() {
        return this == ESTADUAL || this == FEDERAL;
    }

    public boolean admiteAudienciaCustodia() {
        return this == ESTADUAL || this == FEDERAL;
    }
}
