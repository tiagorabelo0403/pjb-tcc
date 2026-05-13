package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;

public record AgravoInterno(
        boolean contraDecisaoMonocratica,
        boolean contraFiltroPresidencial,
        boolean interpostoNoMesmoOrgaoFracionario) implements RecursalSpecies {

    public AgravoInterno {
        if (!contraDecisaoMonocratica) {
            throw new IllegalArgumentException("Agravo interno exige decisão monocrática antecedente");
        }
    }

    @Override
    public String code() {
        return "AGINT";
    }

    @Override
    public String formalName() {
        return "Agravo Interno";
    }

    @Override
    public LegalAppealType legacyType() {
        return LegalAppealType.AGRAVO_INTERNO;
    }

    @Override
    public boolean sameCaseAutos() {
        return true;
    }

    @Override
    public boolean requiresCounterReasons() {
        return false;
    }

    @Override
    public boolean potentiallyRequiresPreparo() {
        return false;
    }

    @Override
    public boolean requiresCollegiateMerit() {
        return true;
    }
}
