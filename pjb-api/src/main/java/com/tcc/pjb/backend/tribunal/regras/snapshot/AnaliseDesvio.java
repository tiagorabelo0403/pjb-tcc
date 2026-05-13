package com.tcc.pjb.backend.tribunal.regras.snapshot;
import com.tcc.pjb.backend.tribunal.regras.TribunalRuleEngine;

public record AnaliseDesvio(
        TribunalRuleEngine.ChaveRegra chave,
        String tribunalCodigo,
        String valorNacional,
        String valorTribunal,
        double percentDesvio,
        TribunalRuleEngine.NivelDesvio nivelDesvio,
        String fundNacional,
        String fundTribunal
) {
    public String descricao() {
        return chave.canonical() + ": Nacional=" + valorNacional + " vs " + tribunalCodigo + "=" + valorTribunal
                + " (desvio " + Math.round(percentDesvio) + "%) [" + nivelDesvio + "]";
    }
}
