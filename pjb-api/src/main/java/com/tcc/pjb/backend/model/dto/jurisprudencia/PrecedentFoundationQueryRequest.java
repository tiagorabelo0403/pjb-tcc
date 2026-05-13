package com.tcc.pjb.backend.model.dto.jurisprudencia;

import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoPrecedente;
import com.tcc.pjb.backend.model.entity.enums.TribunalFonte;

public record PrecedentFoundationQueryRequest(
        Long processoId,
        TribunalFonte fonte,
        TipoPrecedente tipo,
        RamoDireito ramo,
        String rito,
        String consulta,
        Integer page,
        Integer size
) {
}
