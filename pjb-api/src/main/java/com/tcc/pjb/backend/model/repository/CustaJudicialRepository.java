package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.financeiro.CustaJudicial;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustaJudicialRepository extends JpaRepository<CustaJudicial, Long> {
    List<CustaJudicial> findByProcessoIdOrderByCreatedAtDesc(Long processoId);
}
