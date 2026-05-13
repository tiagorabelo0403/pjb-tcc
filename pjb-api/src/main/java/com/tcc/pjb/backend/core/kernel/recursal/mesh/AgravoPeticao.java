package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;

public record AgravoPeticao(
        boolean execucaoGarantidaOuDispensaLegal,
        boolean delimitacaoJustificadaMateriasValores,
        boolean contraDecisaoExecucao,
        boolean execucaoTrabalhista) implements RecursalSpecies {

    public AgravoPeticao {
        if (!contraDecisaoExecucao || !execucaoTrabalhista) {
            throw new IllegalArgumentException("Agravo de petição exige decisão em execução trabalhista");
        }
        if (!execucaoGarantidaOuDispensaLegal || !delimitacaoJustificadaMateriasValores) {
            throw new IllegalArgumentException("Agravo de petição exige garantia ou dispensa legal e delimitação justificada");
        }
    }

    @Override
    public String code() {
        return "AGPET";
    }

    @Override
    public String formalName() {
        return "Agravo de Petição";
    }

    @Override
    public LegalAppealType legacyType() {
        return LegalAppealType.AGRAVO_PETICAO;
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
