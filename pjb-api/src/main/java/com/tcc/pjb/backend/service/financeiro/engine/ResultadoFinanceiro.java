package com.tcc.pjb.backend.service.financeiro.engine;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoFinanceiro {

    
    private String descricao;

    
    private BigDecimal base;

    
    private BigDecimal valorPrincipal;

    
    private BigDecimal tributos;

    
    private BigDecimal totalEstimado;

    
    @Builder.Default
    private Map<String, BigDecimal> componentes = new LinkedHashMap<>();

    
    private String observacoes;

    
    private String observacao;

    
    public void normalize() {
        if (base == null) base = valorPrincipal;
        if (valorPrincipal == null) valorPrincipal = base;
        if (observacoes == null) observacoes = observacao;
        if (observacao == null) observacao = observacoes;

        if (componentes == null) componentes = new LinkedHashMap<>();
        if (base != null && !componentes.containsKey("base")) {
            componentes.put("base", base);
        }
        if (tributos != null) {
            componentes.putIfAbsent("tributos", tributos);
        }

        if (totalEstimado == null) {
            BigDecimal total = BigDecimal.ZERO;
            for (BigDecimal v : componentes.values()) {
                if (v != null) total = total.add(v);
            }
            if (total.signum() == 0 && base != null) total = base;
            totalEstimado = total;
        }
    }

    public Map<String, BigDecimal> getComponentesSafe() {
        return componentes == null ? Collections.emptyMap() : Collections.unmodifiableMap(componentes);
    }

    
    public BigDecimal getBase() {
        return base != null ? base : valorPrincipal;
    }

    
    public BigDecimal getTotalEstimado() {
        return totalEstimado != null ? totalEstimado : (getBase() == null ? BigDecimal.ZERO : getBase());
    }
}
