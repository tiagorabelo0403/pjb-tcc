package com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos;

import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSpecies;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;

public record EmbargosExecucao(
        boolean execucaoGarantidaOuDispensaLegal,
        boolean tempestivoNaExecucao,
        boolean impugnaTituloOuPenhora,
        boolean sameCaseAutos) implements RecursalSpecies {

    public EmbargosExecucao {
        if (!execucaoGarantidaOuDispensaLegal || !tempestivoNaExecucao || !impugnaTituloOuPenhora) {
            throw new IllegalArgumentException("Embargos à execução exigem garantia do juízo, tempestividade e impugnação executiva típica");
        }
    }

    @Override
    public String code() {
        return "EEXEC";
    }

    @Override
    public String formalName() {
        return "Embargos à Execução";
    }

    @Override
    public LegalAppealType legacyType() {
        return LegalAppealType.EMBARGOS_EXECUCAO;
    }

    @Override
    public boolean sameCaseAutos() {
        return sameCaseAutos;
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
        return false;
    }
}
