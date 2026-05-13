package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;

public record RecursoRevista(
        boolean transcendenciaFundamentada,
        boolean violacaoLiteralDispositivoOuDivergencia,
        boolean prequestionamentoExpresso,
        boolean dissidioJurisprudencialEspecifico) implements RecursalSpecies {

    public RecursoRevista {
        if (!transcendenciaFundamentada) {
            throw new IllegalArgumentException("Recurso de revista exige transcendência fundamentada");
        }
        if (!violacaoLiteralDispositivoOuDivergencia && !dissidioJurisprudencialEspecifico) {
            throw new IllegalArgumentException("Recurso de revista exige violação literal ou divergência específica");
        }
        if (!prequestionamentoExpresso) {
            throw new IllegalArgumentException("Recurso de revista exige prequestionamento");
        }
    }

    @Override
    public String code() {
        return "RR";
    }

    @Override
    public String formalName() {
        return "Recurso de Revista";
    }

    @Override
    public LegalAppealType legacyType() {
        return LegalAppealType.RECURSO_REVISTA;
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
        return false;
    }

    @Override
    public boolean requiresCollegiateMerit() {
        return true;
    }
}
