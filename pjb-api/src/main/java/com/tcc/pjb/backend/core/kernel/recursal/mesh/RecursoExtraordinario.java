package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;

public record RecursoExtraordinario(
        boolean demonstracaoQuestaoConstitucional,
        boolean repercussaoGeralFundamentada,
        boolean temaConstitucionalPrequestionado,
        boolean paradigmaRepercussaoGeralVinculante) implements RecursalSpecies {

    public RecursoExtraordinario {
        if (!demonstracaoQuestaoConstitucional) {
            throw new IllegalArgumentException("Recurso extraordinário exige questão constitucional direta");
        }
    }

    @Override
    public String code() {
        return "RE";
    }

    @Override
    public String formalName() {
        return "Recurso Extraordinário";
    }

    @Override
    public LegalAppealType legacyType() {
        return LegalAppealType.RE;
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
