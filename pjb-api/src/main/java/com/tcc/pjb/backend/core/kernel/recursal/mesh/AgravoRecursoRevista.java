package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;

public record AgravoRecursoRevista(
        boolean decisaoDenegatoriaRecursoTrabalhista,
        boolean impugnacaoEspecificaFundamentos,
        boolean decisaoViceOuPresidencia,
        boolean transcendenciaFundamentada) implements RecursalSpecies {

    public AgravoRecursoRevista {
        if (!decisaoDenegatoriaRecursoTrabalhista || !decisaoViceOuPresidencia) {
            throw new IllegalArgumentException("Agravo em recurso de revista exige decisão denegatória da presidência ou vice-presidência");
        }
        if (!impugnacaoEspecificaFundamentos) {
            throw new IllegalArgumentException("Agravo em recurso de revista exige impugnação específica");
        }
        if (!transcendenciaFundamentada) {
            throw new IllegalArgumentException("Agravo em recurso de revista exige demonstração de transcendência");
        }
    }

    @Override
    public String code() {
        return "AIRR";
    }

    @Override
    public String formalName() {
        return "Agravo em Recurso de Revista";
    }

    @Override
    public LegalAppealType legacyType() {
        return LegalAppealType.AGRAVO_RECURSO_REVISTA;
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
