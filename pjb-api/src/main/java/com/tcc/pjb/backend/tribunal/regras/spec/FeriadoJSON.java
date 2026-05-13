package com.tcc.pjb.backend.tribunal.regras.spec;


    public record FeriadoJSON(
            String data,
            String tipo,
            String descricao,
            boolean suspendeExpediente,
            boolean suspendePrazos,
            String recorrencia,
            String fundamentacao,
            String abrangencia,
            String uf,
            String comarca
    ) {}
