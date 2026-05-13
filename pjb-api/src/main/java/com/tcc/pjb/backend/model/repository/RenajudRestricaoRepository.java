package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.financeiro.RenajudRestricao;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RenajudRestricaoRepository extends JpaRepository<RenajudRestricao, Long> {
    List<RenajudRestricao> findByProcessoIdOrderByCreatedAtDesc(Long processoId);

    @Query("""
            select r from RenajudRestricao r
            where r.status = 'FAILED'
              and (r.proximoRetryEm is null or r.proximoRetryEm <= :now)
            order by r.id asc
            """)
    List<RenajudRestricao> findRetryCandidates(@Param("now") Instant now);
}
