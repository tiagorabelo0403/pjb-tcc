package com.tcc.pjb.backend.tribunal.regras.spec;


    public record RegraJSON(
            String chave,
            String nivel,
            String escopoId,
            Object valor,
            String tipo,
            String modo,
            String descricao,
            String fundamentacao,
            String vigenteDesde,
            String vigenteAte,
            String ramo,
            String grau
    ) {}
