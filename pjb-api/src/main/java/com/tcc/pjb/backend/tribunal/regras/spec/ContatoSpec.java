package com.tcc.pjb.backend.tribunal.regras.spec;


    public record ContatoSpec(
            String site,
            String email,
            String telefone,
            String endereco,
            String cep,
            String cidade,
            String uf,
            String horarioAtendimento,
            String ouvidoria
    ) {}
