package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;

public record CorrecaoParcial(
        boolean errorInProcedendoOuTumulto,
        boolean regimentoInternoAutoriza,
        boolean semRecursoProprioEficaz,
        boolean decisaoIrrrecorrivelNaViaOrdinaria) implements RecursalSpecies {

    public CorrecaoParcial {
        if (!errorInProcedendoOuTumulto || !regimentoInternoAutoriza) {
            throw new IllegalArgumentException("Correição parcial exige tumulto processual e previsão regimental");
        }
        if (!semRecursoProprioEficaz && !decisaoIrrrecorrivelNaViaOrdinaria) {
            throw new IllegalArgumentException("Correição parcial exige ausência de recurso próprio eficaz");
        }
    }

    @Override
    public String code() {
        return "CPARCIAL";
    }

    @Override
    public String formalName() {
        return "Correição Parcial";
    }

    @Override
    public LegalAppealType legacyType() {
        return LegalAppealType.CORREICAO_PARCIAL;
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
