package com.tcc.pjb.backend.core.quality.apisurface.domain;

import java.util.List;

public record PjbApiSurfaceIssue(
        String codigo,
        String severidade,
        String alvo,
        String verbo,
        String rota,
        List<String> detalhes
) {


    public String code() {
        return codigo;
    }

    public String location() {
        return alvo;
    }
}
