package com.tcc.pjb.backend.tribunal.regras.spec;


    public record CalendarioEntrySpec(
            String data,
            String tipo,
            String descricao,
            Boolean suspendeExpediente,
            Boolean suspendePrazos,
            String recorrencia,
            String fundamentacao,
            String abrangencia,
            String uf,
            String comarca
    ) {}
