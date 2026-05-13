package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;

public record PedidoUniformizacaoFederal(
        boolean divergenciaEntreTurmasRecursaisOuRegionais,
        boolean contrariedadeJurisprudenciaDominante,
        boolean paradigmaComprovado,
        boolean impugnacaoEspecificaFundamentos) implements RecursalSpecies {

    public PedidoUniformizacaoFederal {
        if (!divergenciaEntreTurmasRecursaisOuRegionais && !contrariedadeJurisprudenciaDominante) {
            throw new IllegalArgumentException("Pedido de uniformização exige divergência ou contrariedade jurisprudencial dominante");
        }
    }

    @Override
    public String code() {
        return "PUILF";
    }

    @Override
    public String formalName() {
        return "Pedido de Uniformização de Interpretação de Lei Federal";
    }

    @Override
    public LegalAppealType legacyType() {
        return LegalAppealType.PEDIDO_UNIFORMIZACAO;
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
