package com.tcc.pjb.backend.modules.custas.api;

import com.tcc.pjb.backend.modules.custas.infrastructure.persistence.CustaJudicial;
import java.util.Optional;

public interface CustaJudicialStorePort {

    Optional<CustaJudicial> findById(Long custaId);

    CustaJudicial save(CustaJudicial entity);
}
