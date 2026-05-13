package com.tcc.pjb.backend.service.financeiro.engine;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.service.financeiro.fiscal.SplitPaymentEngine;

@Component
public class TributarioFinanceEngine implements FinanceEngine {

    private final SplitPaymentEngine splitEngine;

    public TributarioFinanceEngine(SplitPaymentEngine splitEngine) {
        this.splitEngine = splitEngine;
    }

    @Override
    public MateriaJurisdicao supports() {
        return MateriaJurisdicao.TRIBUTARIA;
    }

    @Override
    public ResultadoFinanceiro calcular(Processo processo) {

        BigDecimal valorCausa = processo.getValorCausa() == null ? BigDecimal.ZERO : processo.getValorCausa();

        BigDecimal ibs = valorCausa.multiply(new BigDecimal("0.17"));
        BigDecimal cbs = valorCausa.multiply(new BigDecimal("0.09"));

        splitEngine.validarSplitPayment(processo);

        ResultadoFinanceiro r = ResultadoFinanceiro.builder()
                .descricao("Tributário – IBS/CBS + Split Payment")
                .base(valorCausa)
                .tributos(ibs.add(cbs))
                .componentes(Map.of(
                        "base", valorCausa,
                        "ibs_estimado", ibs,
                        "cbs_estimado", cbs
                ))
                .observacoes("Cálculo estimado conforme EC 132/2023; alíquotas e regras podem variar por lei complementar.")
                .build();
        r.normalize();
        return r;
    }
}
