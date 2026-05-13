package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.catalog.TpuClasseCnj;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.Objects;

record NationalProceduralForumAllocationClassSeed(
        RitoProcessual rito,
        TpuClasseCnj classeTpu
) {

    NationalProceduralForumAllocationClassSeed {
        Objects.requireNonNull(rito);
    }
}
