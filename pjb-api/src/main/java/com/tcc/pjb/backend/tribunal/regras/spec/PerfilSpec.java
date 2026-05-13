package com.tcc.pjb.backend.tribunal.regras.spec;

import java.util.Map;


    public record PerfilSpec(
            String tribunalNome,
            String tribunalSigla,
            String uf,
            String ramo,
            String grau,
            VisualSpec visual,
            Map<String, String> terminologia,
            Map<String, String> terminologiaPlural,
            UxSpec ux,
            ContatoSpec contato,
            Boolean tornarAtivo
    ) {}
