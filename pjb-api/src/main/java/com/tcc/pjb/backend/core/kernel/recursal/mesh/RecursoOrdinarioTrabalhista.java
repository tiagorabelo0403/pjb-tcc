package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;

public record RecursoOrdinarioTrabalhista(
        boolean contraSentencaOuAcordaoOriginario,
        boolean dissidioIndividualOuColetivo,
        boolean preparoRecursalRegular) implements RecursalSpecies {

    public RecursoOrdinarioTrabalhista {
        if (!contraSentencaOuAcordaoOriginario || !dissidioIndividualOuColetivo) {
            throw new IllegalArgumentException("Recurso ordinário trabalhista exige decisão originária trabalhista recorrível");
        }
    }

    @Override
    public String code() {
        return "ROT";
    }

    @Override
    public String formalName() {
        return "Recurso Ordinário Trabalhista";
    }

    @Override
    public LegalAppealType legacyType() {
        return LegalAppealType.RECURSO_ORDINARIO_TRABALHISTA;
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
        return preparoRecursalRegular;
    }

    @Override
    public boolean requiresCollegiateMerit() {
        return true;
    }
}
