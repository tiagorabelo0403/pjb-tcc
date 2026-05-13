package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;

public record ConflitoCompetencia(
        boolean orgaosDistintosEmColisao,
        boolean conflitoPositivoOuNegativo,
        boolean ausenciaHierarquiaComumImediata,
        boolean necessitaDefinicaoCompetencia) implements RecursalSpecies {

    public ConflitoCompetencia {
        if (!orgaosDistintosEmColisao || !conflitoPositivoOuNegativo) {
            throw new IllegalArgumentException("Conflito de competência exige colisão positiva ou negativa entre órgãos distintos");
        }
        if (!necessitaDefinicaoCompetencia) {
            throw new IllegalArgumentException("Conflito de competência exige necessidade de definição jurisdicional");
        }
    }

    @Override
    public String code() {
        return "CC";
    }

    @Override
    public String formalName() {
        return "Conflito de Competência";
    }

    @Override
    public LegalAppealType legacyType() {
        return LegalAppealType.CONFLITO_COMPETENCIA;
    }

    @Override
    public boolean sameCaseAutos() {
        return false;
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
