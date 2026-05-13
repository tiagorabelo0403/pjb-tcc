package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;

public record AgravoInstrumentoTrabalhista(
        boolean decisaoDenegatoriaRecursoTrabalhista,
        boolean impugnacaoEspecificaFundamentos,
        boolean formaInstrumentoRegular) implements RecursalSpecies {

    public AgravoInstrumentoTrabalhista {
        if (!decisaoDenegatoriaRecursoTrabalhista) {
            throw new IllegalArgumentException("Agravo de instrumento trabalhista exige decisão denegatória de recurso");
        }
        if (!impugnacaoEspecificaFundamentos || !formaInstrumentoRegular) {
            throw new IllegalArgumentException("Agravo de instrumento trabalhista exige regularidade formal e impugnação específica");
        }
    }

    @Override
    public String code() {
        return "AGITRAB";
    }

    @Override
    public String formalName() {
        return "Agravo de Instrumento Trabalhista";
    }

    @Override
    public LegalAppealType legacyType() {
        return LegalAppealType.AGRAVO_INSTRUMENTO;
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
