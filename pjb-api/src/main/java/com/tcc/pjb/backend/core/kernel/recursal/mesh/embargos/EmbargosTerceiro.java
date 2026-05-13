package com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos;

import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSpecies;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;

public record EmbargosTerceiro(
        boolean terceiroEstranhoRelacaoProcessual,
        boolean constricaoSobreBemDeTerceiro,
        boolean posseOuDominioComprovavel,
        boolean tempestivoConstricao) implements RecursalSpecies {

    public EmbargosTerceiro {
        if (!terceiroEstranhoRelacaoProcessual || !constricaoSobreBemDeTerceiro) {
            throw new IllegalArgumentException("Embargos de terceiro exigem constrição sobre bem de terceiro estranho à relação processual");
        }
        if (!posseOuDominioComprovavel || !tempestivoConstricao) {
            throw new IllegalArgumentException("Embargos de terceiro exigem prova possessória ou dominial e tempestividade");
        }
    }

    @Override
    public String code() {
        return "ETERC";
    }

    @Override
    public String formalName() {
        return "Embargos de Terceiro";
    }

    @Override
    public LegalAppealType legacyType() {
        return LegalAppealType.EMBARGOS_TERCEIRO;
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
        return false;
    }
}
