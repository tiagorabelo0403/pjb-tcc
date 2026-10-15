package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.catalog.TpuClasseCnj;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RegimeDistribuicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

public record RegimeDistribuicaoDecisao(
        RegimeDistribuicao regime,
        RamoDireito ramoDireito,
        TpuClasseCnj.RamoJustica ramoJustica,
        GrauJurisdicao grau,
        RitoProcessual rito,
        String fundamentoLegal
) {
}
