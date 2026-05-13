package com.tcc.pjb.backend.service.financeiro.engine;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;

@Component
public class AdministrativoFinanceEngine implements FinanceEngine {

    @Override
    public MateriaJurisdicao supports() {
        return MateriaJurisdicao.ADMINISTRATIVO;
    }

    @Override
    public ResultadoFinanceiro calcular(Processo processo) {
        BigDecimal base = processo.getValorCausa() == null ? BigDecimal.ZERO : processo.getValorCausa();

        
        BigDecimal taxas = base.signum() > 0 ? base.multiply(new BigDecimal("0.008")) : BigDecimal.ZERO;

        ResultadoFinanceiro r = ResultadoFinanceiro.builder()
                .descricao("Administrativo – estimativa")
                .base(base)
                .tributos(taxas)
                .componentes(Map.of(
                        "base", base,
                        "taxas_estimadas", taxas
                ))
                .observacoes("Administrativo: custos dependem de legislação local e natureza do ato.")
                .build();
        r.normalize();
        return r;
    }
}
