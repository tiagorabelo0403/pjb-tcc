package com.tcc.pjb.backend.modules.custas.api;

import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

public record ProcessoCustaContexto(Long processoId, String uf, RamoDireito ramoDireito, RitoProcessual rito) {
}
