package com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos;

import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSpecies;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;

public record EmbargosExecucaoFiscal(
        boolean execucaoGarantidaOuDispensaLegal,
        boolean tempestivoNaExecucaoFiscal,
        boolean creditoFiscalConstituido,
        boolean garantiaIntegralOuSuficiente) implements RecursalSpecies {

    public EmbargosExecucaoFiscal {
        if (!execucaoGarantidaOuDispensaLegal || !garantiaIntegralOuSuficiente) {
            throw new IllegalArgumentException("Embargos à execução fiscal exigem garantia ou hipótese legal equivalente");
        }
        if (!tempestivoNaExecucaoFiscal || !creditoFiscalConstituido) {
            throw new IllegalArgumentException("Embargos à execução fiscal exigem crédito constituído e tempestividade");
        }
    }

    @Override
    public String code() {
        return "EEFISC";
    }

    @Override
    public String formalName() {
        return "Embargos à Execução Fiscal";
    }

    @Override
    public LegalAppealType legacyType() {
        return LegalAppealType.EMBARGOS_EXECUCAO_FISCAL;
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
