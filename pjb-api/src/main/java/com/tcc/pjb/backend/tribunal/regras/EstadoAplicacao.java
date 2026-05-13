package com.tcc.pjb.backend.tribunal.regras;

import java.util.LinkedHashSet;

record EstadoAplicacao(
        Object valor,
        TribunalRuleEngine.TipoValor tipoValor,
        TribunalRuleEngine.EntradaRegra fonte,
        boolean restringirAplicado,
        LinkedHashSet<String> trilha
) {}
