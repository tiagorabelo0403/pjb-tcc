package com.tcc.pjb.backend.tribunal.regras.spec;


    public record CalendarioRecessoSpec(
            String descricao,
            String inicio,
            String fim,
            Boolean suspendePrazos,
            String fundamentacao,
            String uf,
            String comarca
    ) {}
