package com.tcc.pjb.backend.tribunal.regras;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

record RulePackSyncBucket(String tribunalCodigo, RamoDireito ramo, GrauJurisdicao grau) {}
