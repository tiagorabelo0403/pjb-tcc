package com.tcc.pjb.backend.service.financeiro.engine;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;

@Component
public class TrabalhistaFinanceEngine implements FinanceEngine {

    @Override
    public MateriaJurisdicao supports() {
        return MateriaJurisdicao.TRABALHISTA;
    }

    @Override
    public ResultadoFinanceiro calcular(Processo processo) {
        BigDecimal base = processo.getValorCausa() == null ? BigDecimal.ZERO : processo.getValorCausa();

        
        BigDecimal custasEstimadas = base.multiply(new BigDecimal("0.02"));

        ResultadoFinanceiro r = ResultadoFinanceiro.builder()
                .descricao("Trabalhista – estimativa")
                .base(base)
                .componentes(Map.of(
                        "base", base,
                        "custas_estimadas", custasEstimadas
                ))
                .totalEstimado(base.add(custasEstimadas))
                .observacoes("Estimativa. Custas e depósitos recursais dependem de regras do TRT/TST e da fase.")
                .build();
        r.normalize();
        return r;
    }
}
