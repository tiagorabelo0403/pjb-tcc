package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.util.Objects;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;

public record AgravoRecursoEspecial(
        boolean impugnaNegativaSeguimento,
        boolean impugnaInadmissao,
        boolean impugnacaoEspecificaFundamentos,
        boolean decisaoViceOuPresidencia,
        boolean demonstraViolacaoLeiFederal) implements RecursalSpecies {

    public AgravoRecursoEspecial {
        require(impugnaNegativaSeguimento || impugnaInadmissao, "Agravo em recurso especial exige impugnação de negativa de seguimento ou inadmissão");
        require(impugnacaoEspecificaFundamentos, "Agravo em recurso especial exige impugnação específica dos fundamentos da decisão agravada");
        require(decisaoViceOuPresidencia, "Agravo em recurso especial exige decisão da presidência ou vice-presidência do tribunal de origem");
        require(demonstraViolacaoLeiFederal, "Agravo em recurso especial exige substrato federal infraconstitucional");
    }

    @Override
    public String code() {
        return "ARESP";
    }

    @Override
    public String formalName() {
        return "Agravo em Recurso Especial";
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
