package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;

public record ApelacaoCivel(
        boolean contraSentenca,
        boolean sujeitoAoReexameNecessario,
        boolean materiaFazendaria,
        boolean sentencaParcialMerito) implements RecursalSpecies {

    public ApelacaoCivel {
        if (!contraSentenca) {
            throw new IllegalArgumentException("Apelação cível exige sentença ou decisão com regime de apelação");
        }
    }

    @Override
    public String code() {
        return "APCIV";
    }

    @Override
    public String formalName() {
        return "Apelação Cível";
    }

    @Override
    public LegalAppealType legacyType() {
        return LegalAppealType.APELACAO;
    }

    @Override
    public boolean sameCaseAutos() {
        return false;
    }

    @Override
    public boolean requiresCounterReasons() {
        return true;
    }

    @Override
    public boolean potentiallyRequiresPreparo() {
        return true;
    }

    @Override
    public boolean requiresCollegiateMerit() {
        return true;
    }
}
