package com.tcc.pjb.backend.tribunal.regras.spec;


    public record TribunalRuleSpec(
            String path,
            String nivel,
            String escopoId,
            Object valor,
            String tipoValor,
            String modo,
            String fundamentacao,
            String descricao,
            Boolean ativa,
            String vigenteDesde,
            String vigenteAte,
            String versao,
            String ramo,
            String grau
    ) {}
