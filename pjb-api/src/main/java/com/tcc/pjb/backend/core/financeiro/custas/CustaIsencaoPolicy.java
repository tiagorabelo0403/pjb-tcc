package com.tcc.pjb.backend.core.financeiro.custas;

import com.tcc.pjb.backend.core.financeiro.custas.domain.IsencaoCustaResult;
import com.tcc.pjb.backend.core.financeiro.custas.domain.TipoCusta;
import com.tcc.pjb.backend.model.entity.Processo;

public interface CustaIsencaoPolicy {
    IsencaoCustaResult verificar(Processo processo, TipoCusta tipoCusta);
}
