package com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos;

import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSpecies;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;

public record EmbargosDivergencia(
        boolean divergenciaEntreOrgaosFracionarios,
        boolean paradigmaComprovado,
        boolean meritoDoParadigmaConhecido,
        boolean acordaoEmbargadoEmCompetenciaSuperior) implements RecursalSpecies {

    public EmbargosDivergencia {
        if (!divergenciaEntreOrgaosFracionarios || !paradigmaComprovado) {
            throw new IllegalArgumentException("Embargos de divergência exigem demonstração analítica de divergência interna");
        }
    }

    @Override
    public String code() {
        return "EDIV";
    }

    @Override
    public String formalName() {
        return "Embargos de Divergência";
    }

    @Override
    public LegalAppealType legacyType() {
        return LegalAppealType.OUTRO;
    }

    @Override
    public boolean sameCaseAutos() {
        return true;
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
