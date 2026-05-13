package com.tcc.pjb.backend.service.jurisprudencia.search;

import java.time.LocalDate;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoPrecedente;
import com.tcc.pjb.backend.model.entity.enums.TribunalFonte;

public record JurisprudenceSearchHit(
        Long id,
        TribunalFonte fonte,
        TipoPrecedente tipo,
        String identificador,
        String titulo,
        String tese,
        String ementaResumo,
        String urlReferencia,
        LocalDate dataPublicacao,
        RamoDireito ramoSugerido,
        RitoProcessual ritoSugerido,
        double score
) {}
