package com.tcc.pjb.backend.service.financeiro.engine;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;

@Component
public class CivilFinanceEngine implements FinanceEngine {

    @Override
    public MateriaJurisdicao supports() {
        return MateriaJurisdicao.CIVIL;
    }

    @Override
    public ResultadoFinanceiro calcular(Processo processo) {
        BigDecimal base = processo.getValorCausa() == null ? BigDecimal.ZERO : processo.getValorCausa();

        
        BigDecimal custasEstimadas = base.multiply(new BigDecimal("0.01"));

        ResultadoFinanceiro r = ResultadoFinanceiro.builder()
                .descricao("Cível – estimativa de custas")
                .base(base)
                .componentes(Map.of(
                        "base", base,
                        "custas_estimadas", custasEstimadas
                ))
                .totalEstimado(base.add(custasEstimadas))
                .observacoes("Estimativa simplificada: custas variam por UF/tribunal. Ajustar por tabela oficial.")
                .build();
        r.normalize();
        return r;
    }
}
