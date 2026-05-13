package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;

public record RecursoInominadoJuizado(
        boolean contraSentencaJuizado,
        boolean tempestivoMicrossistema,
        boolean preparoMicrossistemaRegular,
        boolean classFamilyJuizado) implements RecursalSpecies {

    public RecursoInominadoJuizado {
        if (!contraSentencaJuizado || !classFamilyJuizado) {
            throw new IllegalArgumentException("Recurso inominado exige sentença de juizado especial");
        }
        if (!tempestivoMicrossistema) {
            throw new IllegalArgumentException("Recurso inominado exige tempestividade no microssistema");
        }
    }

    @Override
    public String code() {
        return "RINOM";
    }

    @Override
    public String formalName() {
        return "Recurso Inominado";
    }

    @Override
    public LegalAppealType legacyType() {
        return LegalAppealType.RECURSO_INOMINADO;
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
        return preparoMicrossistemaRegular;
    }

    @Override
    public boolean requiresCollegiateMerit() {
        return true;
    }
}
