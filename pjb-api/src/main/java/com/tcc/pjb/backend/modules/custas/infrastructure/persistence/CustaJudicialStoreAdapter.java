package com.tcc.pjb.backend.modules.custas.infrastructure.persistence;

import com.tcc.pjb.backend.modules.custas.api.CustaJudicialStorePort;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CustaJudicialStoreAdapter implements CustaJudicialStorePort {

    private final CustaJudicialRepository custaJudicialRepository;

    public CustaJudicialStoreAdapter(CustaJudicialRepository custaJudicialRepository) {
        this.custaJudicialRepository = Objects.requireNonNull(custaJudicialRepository);
    }

    @Override
    public Optional<CustaJudicial> findById(Long custaId) {
        return custaJudicialRepository.findById(custaId);
    }

    @Override
    public CustaJudicial save(CustaJudicial entity) {
        return custaJudicialRepository.save(entity);
    }
}
