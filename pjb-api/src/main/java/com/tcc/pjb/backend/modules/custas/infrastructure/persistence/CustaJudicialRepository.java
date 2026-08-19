package com.tcc.pjb.backend.modules.custas.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustaJudicialRepository extends JpaRepository<CustaJudicial, Long> {
    List<CustaJudicial> findByProcessoIdOrderByCreatedAtDesc(Long processoId);
}
