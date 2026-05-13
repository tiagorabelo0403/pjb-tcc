package com.tcc.pjb.backend.tribunal.regras.spec;

import java.util.List;


    public record RulePackSpec(
            String tipo,
            String codigo,
            String descricao,
            String ramo,
            String grau,
            String fundamento,
            List<String> requisitos,
            String tipoAto,
            Integer dias,
            Boolean uteis,
            List<String> documentosObrigatorios,
            Boolean bloqueante,
            String mensagemAlerta,
            String nivelAlerta,
            String faseOrigem,
            String proximaFase,
            Boolean exigeAprovacao
    ) {}
