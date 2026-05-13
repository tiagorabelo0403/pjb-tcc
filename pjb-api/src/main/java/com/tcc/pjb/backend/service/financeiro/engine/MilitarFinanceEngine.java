package com.tcc.pjb.backend.service.financeiro.engine;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;

@Component
public class MilitarFinanceEngine implements FinanceEngine {

    @Override
    public MateriaJurisdicao supports() {
        return MateriaJurisdicao.MILITAR;
    }

    @Override
    public ResultadoFinanceiro calcular(Processo processo) {
        BigDecimal base = processo.getValorCausa() == null ? BigDecimal.ZERO : processo.getValorCausa();

        ResultadoFinanceiro r = ResultadoFinanceiro.builder()
                .descricao("Militar – estimativa")
                .base(base)
                .totalEstimado(base)
                .componentes(Map.of("base_referencia", base))
                .observacoes("Militar: custos e parâmetros dependem do objeto (disciplinar, penal militar, administrativo).")
                .build();
        r.normalize();
        return r;
    }
}
