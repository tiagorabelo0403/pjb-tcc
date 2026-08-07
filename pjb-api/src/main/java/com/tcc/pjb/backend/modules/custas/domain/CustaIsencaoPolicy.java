package com.tcc.pjb.backend.modules.custas.domain;

import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

public interface CustaIsencaoPolicy {
    IsencaoCustaResult verificar(RamoDireito ramoDireito, RitoProcessual rito, TipoCusta tipoCusta);
}
