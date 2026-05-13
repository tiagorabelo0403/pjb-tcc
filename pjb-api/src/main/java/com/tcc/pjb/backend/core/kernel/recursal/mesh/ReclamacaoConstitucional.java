package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;

public record ReclamacaoConstitucional(
        boolean preservaCompetenciaTribunal,
        boolean garanteAutoridadePrecedente,
        boolean atoContrarioPrecedenteVinculante,
        boolean origemEmAutoridadeSubordinada) implements RecursalSpecies {

    public ReclamacaoConstitucional {
        if (!preservaCompetenciaTribunal && !garanteAutoridadePrecedente && !atoContrarioPrecedenteVinculante) {
            throw new IllegalArgumentException("Reclamação exige fundamento de preservação de competência ou autoridade decisória");
        }
        if (!origemEmAutoridadeSubordinada) {
            throw new IllegalArgumentException("Reclamação exige ato oriundo de autoridade subordinada ou vinculada");
        }
    }

    @Override
    public String code() {
        return "RCL";
    }

    @Override
    public String formalName() {
        return "Reclamação";
    }

    @Override
    public LegalAppealType legacyType() {
        return LegalAppealType.RECLAMACAO_CONSTITUCIONAL;
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
