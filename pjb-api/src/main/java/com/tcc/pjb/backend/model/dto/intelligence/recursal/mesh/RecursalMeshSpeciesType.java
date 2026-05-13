package com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalAliasRegistry;

public enum RecursalMeshSpeciesType {
    EDCL,
    AGINT,
    APCIV,
    APCRIM,
    AGINST,
    AGITRAB,
    AGREG,
    ROC,
    ROT,
    RR,
    AIRR,
    AGPET,
    EEXEC,
    EEFISC,
    ETERC,
    RESP,
    RE,
    ARESP,
    ARE,
    EDIV,
    RCL,
    CC,
    CPARCIAL,
    RINOM,
    PUILF;

    private static final RecursalAliasRegistry ALIASES = new RecursalAliasRegistry();

    @JsonCreator
    public static RecursalMeshSpeciesType fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String token = ALIASES.resolve(raw);
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return RecursalMeshSpeciesType.valueOf(token);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
