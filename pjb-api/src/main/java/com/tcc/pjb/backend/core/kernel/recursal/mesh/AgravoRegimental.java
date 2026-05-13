package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;

public record AgravoRegimental(
        boolean contraDecisaoUnipessoal,
        boolean interpostoPeranteColegiadoCompetente,
        boolean regimentoInternoAutoriza) implements RecursalSpecies {

    public AgravoRegimental {
        if (!contraDecisaoUnipessoal) {
            throw new IllegalArgumentException("Agravo regimental exige decisão unipessoal antecedente");
        }
        if (!interpostoPeranteColegiadoCompetente || !regimentoInternoAutoriza) {
            throw new IllegalArgumentException("Agravo regimental exige previsão regimental e colegiado competente");
        }
    }

    @Override
    public String code() {
        return "AGREG";
    }

    @Override
    public String formalName() {
        return "Agravo Regimental";
    }

    @Override
    public LegalAppealType legacyType() {
        return LegalAppealType.AGRAVO_REGIMENTAL;
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
