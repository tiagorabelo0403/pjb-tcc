package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;

public record RecursoEspecial(
        boolean demonstracaoViolacaoLeiFederal,
        boolean prequestionamentoExpresso,
        boolean potencialRepetitivo,
        boolean fundadoEmDissidioJurisprudencial) implements RecursalSpecies {

    public RecursoEspecial {
        if (!demonstracaoViolacaoLeiFederal && !fundadoEmDissidioJurisprudencial) {
            throw new IllegalArgumentException("Recurso especial exige violação de lei federal ou dissídio jurisprudencial");
        }
    }

    @Override
    public String code() {
        return "RESP";
    }

    @Override
    public String formalName() {
        return "Recurso Especial";
    }

    @Override
    public LegalAppealType legacyType() {
        return LegalAppealType.RESP;
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
        return true;
    }

    @Override
    public boolean requiresCollegiateMerit() {
        return true;
    }
}
