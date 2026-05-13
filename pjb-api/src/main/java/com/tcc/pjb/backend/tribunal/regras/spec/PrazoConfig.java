package com.tcc.pjb.backend.tribunal.regras.spec;

import java.util.List;


    public record PrazoConfig(
            Boolean contarSabado,
            Boolean integralmenteCorrido,
            List<String> feriadosAdicionais
    ) {}
