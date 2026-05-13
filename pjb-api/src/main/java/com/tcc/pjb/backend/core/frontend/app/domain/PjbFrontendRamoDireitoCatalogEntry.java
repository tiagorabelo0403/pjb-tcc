package com.tcc.pjb.backend.core.frontend.app.domain;

public record PjbFrontendRamoDireitoCatalogEntry(
        String code,
        String name,
        String descricao,
        String categoria,
        String verticalPrincipal,
        boolean admiteConciliacao,
        boolean exigeAtuacaoMP,
        boolean geraSigiloAutomatico
) {
}
