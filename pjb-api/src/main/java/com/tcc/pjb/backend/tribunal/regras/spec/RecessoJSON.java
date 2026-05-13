package com.tcc.pjb.backend.tribunal.regras.spec;


    public record RecessoJSON(
            String descricao,
            String inicio,
            String fim,
            boolean suspendePrazos,
            String fundamentacao,
            String uf,
            String comarca
    ) {}
