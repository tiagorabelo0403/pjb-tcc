package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;

public record RecursoOrdinarioConstitucional(
        boolean contraDenegacaoMandadoSeguranca,
        boolean origemEmTribunal,
        boolean materiaConstitucionalOuFundamental) implements RecursalSpecies {

    public RecursoOrdinarioConstitucional {
        if (!contraDenegacaoMandadoSeguranca || !origemEmTribunal) {
            throw new IllegalArgumentException("Recurso ordinário constitucional exige decisão denegatória originária em tribunal");
        }
        if (!materiaConstitucionalOuFundamental) {
            throw new IllegalArgumentException("Recurso ordinário constitucional exige matéria constitucional ou fundamental");
        }
    }

    @Override
    public String code() {
        return "ROC";
    }

    @Override
    public String formalName() {
        return "Recurso Ordinário Constitucional";
    }

    @Override
    public LegalAppealType legacyType() {
        return LegalAppealType.RECURSO_ORDINARIO_CONSTITUCIONAL;
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
