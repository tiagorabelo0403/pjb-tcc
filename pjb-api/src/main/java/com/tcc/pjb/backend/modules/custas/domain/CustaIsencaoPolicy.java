package com.tcc.pjb.backend.modules.custas.domain;

import com.tcc.pjb.backend.model.entity.Processo;

public interface CustaIsencaoPolicy {
    IsencaoCustaResult verificar(Processo processo, TipoCusta tipoCusta);
}
