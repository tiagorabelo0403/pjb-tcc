package com.tcc.pjb.backend.modules.custas.api;

import com.tcc.pjb.backend.modules.custas.infrastructure.persistence.CustaJudicial;
import java.util.List;
import java.util.Optional;

public interface CustaJudicialStorePort {

    Optional<CustaJudicial> findById(Long custaId);

    List<CustaJudicial> findByProcessoId(Long processoId);

    CustaJudicial save(CustaJudicial entity);
}
