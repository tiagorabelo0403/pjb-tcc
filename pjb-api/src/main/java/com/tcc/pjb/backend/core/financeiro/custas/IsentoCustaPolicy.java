package com.tcc.pjb.backend.core.financeiro.custas;

import com.tcc.pjb.backend.core.financeiro.custas.domain.IsencaoCustaResult;
import com.tcc.pjb.backend.model.entity.Processo;

public interface IsentoCustaPolicy {
    IsencaoCustaResult verificar(Processo processo, String tipoCusta);
}
