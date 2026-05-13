package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.util.Objects;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;

public record AgravoRecursoExtraordinario(
        boolean impugnaNegativaSeguimento,
        boolean impugnaInadmissao,
        boolean impugnacaoEspecificaFundamentos,
        boolean decisaoViceOuPresidencia,
        boolean demonstraQuestaoConstitucional,
        boolean repercussaoGeralFundamentada) implements RecursalSpecies {

    public AgravoRecursoExtraordinario {
        require(impugnaNegativaSeguimento || impugnaInadmissao, "Agravo em recurso extraordinário exige impugnação de negativa de seguimento ou inadmissão");
        require(impugnacaoEspecificaFundamentos, "Agravo em recurso extraordinário exige impugnação específica dos fundamentos da decisão agravada");
        require(decisaoViceOuPresidencia, "Agravo em recurso extraordinário exige decisão da presidência ou vice-presidência do tribunal de origem");
        require(demonstraQuestaoConstitucional, "Agravo em recurso extraordinário exige questão constitucional direta");
        require(repercussaoGeralFundamentada, "Agravo em recurso extraordinário exige repercussão geral fundamentada");
    }

    @Override
    public String code() {
        return "ARE";
    }

    @Override
    public String formalName() {
        return "Agravo em Recurso Extraordinário";
    }

    @Override
    public LegalAppealType legacyType() {
        return LegalAppealType.AGRAVO_RESP_RE;
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
        return true;
    }

    @Override
    public boolean requiresCollegiateMerit() {
        return true;
    }

    private static void require(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(Objects.requireNonNull(message, "message"));
        }
    }
}
