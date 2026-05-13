package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import java.util.Objects;

record NationalProceduralRoutingClassificationSnapshot(
        String complexityBand,
        String ritoSugerido,
        TipoJustica tipoJustica,
        String proceduralRegime,
        String proceduralTrack
) {

    NationalProceduralRoutingClassificationSnapshot {
        Objects.requireNonNull(complexityBand);
        Objects.requireNonNull(ritoSugerido);
        Objects.requireNonNull(tipoJustica);
        Objects.requireNonNull(proceduralRegime);
        Objects.requireNonNull(proceduralTrack);
    }
}
