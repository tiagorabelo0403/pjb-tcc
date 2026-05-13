package com.tcc.pjb.backend.service.financeiro.engine;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;

@Component
public class PenalFinanceEngine implements FinanceEngine {

    @Override
    public MateriaJurisdicao supports() {
        return MateriaJurisdicao.PENAL;
    }

    @Override
    public ResultadoFinanceiro calcular(Processo processo) {
        BigDecimal base = processo.getValorCausa() == null ? BigDecimal.ZERO : processo.getValorCausa();
        BigDecimal custas = base.multiply(new BigDecimal("0.01"));

        ResultadoFinanceiro r = ResultadoFinanceiro.builder()
                .descricao("Penal: estimativa de custas e reparação")
                .base(base)
                .componentes(Map.of(
                        "base_referencia", base,
                        "custas_estimadas", custas
                ))
                .observacoes("No penal, valores variam por tipo de ação e comarca; estimativa serve para planejamento.")
                .build();
        r.normalize();
        return r;
    }
}
