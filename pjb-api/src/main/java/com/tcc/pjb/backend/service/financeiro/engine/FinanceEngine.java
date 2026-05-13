package com.tcc.pjb.backend.service.financeiro.engine;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;

public interface FinanceEngine {

    MateriaJurisdicao supports();

    ResultadoFinanceiro calcular(Processo processo);
}
