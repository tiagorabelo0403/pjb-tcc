package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.financeiro.InfojudConsulta;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InfojudConsultaRepository extends JpaRepository<InfojudConsulta, Long> {
    List<InfojudConsulta> findByProcessoIdOrderByCreatedAtDesc(Long processoId);

    @Query("""
            select c from InfojudConsulta c
            where c.status = 'FAILED'
              and (c.proximoRetryEm is null or c.proximoRetryEm <= :now)
            order by c.id asc
            """)
    List<InfojudConsulta> findRetryCandidates(@Param("now") Instant now);
}
