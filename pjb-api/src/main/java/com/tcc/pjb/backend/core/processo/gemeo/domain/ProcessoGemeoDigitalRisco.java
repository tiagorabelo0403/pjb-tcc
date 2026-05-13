package com.tcc.pjb.backend.core.processo.gemeo.domain;

import java.util.Objects;

public record ProcessoGemeoDigitalRisco(String codigo,
                                        String nivel,
                                        String descricao,
                                        String impacto,
                                        String proximoAtoSugerido) {
    public ProcessoGemeoDigitalRisco {
        codigo = Objects.toString(codigo, "").trim();
        nivel = Objects.toString(nivel, "").trim();
        descricao = Objects.toString(descricao, "").trim();
        impacto = Objects.toString(impacto, "").trim();
        proximoAtoSugerido = Objects.toString(proximoAtoSugerido, "").trim();
    }
}
