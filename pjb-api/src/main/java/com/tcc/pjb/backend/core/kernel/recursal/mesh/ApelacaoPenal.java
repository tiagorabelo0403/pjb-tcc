package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;

public record ApelacaoPenal(
        boolean contraSentenca,
        boolean tribunalDoJuri,
        boolean recorrenteMinisterioPublico,
        boolean impugnaPronunciaOuDosimetria) implements RecursalSpecies {

    public ApelacaoPenal {
        if (!contraSentenca) {
            throw new IllegalArgumentException("Apelação penal exige sentença penal recorrível");
        }
    }

    @Override
    public String code() {
        return "APCRIM";
    }

    @Override
    public String formalName() {
        return "Apelação Penal";
    }

    @Override
    public LegalAppealType legacyType() {
        return LegalAppealType.APELACAO_PENAL;
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
