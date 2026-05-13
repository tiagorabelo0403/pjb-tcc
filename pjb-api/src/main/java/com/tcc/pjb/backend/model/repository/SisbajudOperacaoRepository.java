package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.financeiro.SisbajudOperacao;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SisbajudOperacaoRepository extends JpaRepository<SisbajudOperacao, Long> {
    List<SisbajudOperacao> findByProcessoIdOrderByCreatedAtDesc(Long processoId);

    @Query("""
            select o from SisbajudOperacao o
            where o.status = 'FAILED'
              and o.cpfDevedor is not null
              and (o.proximoRetryEm is null or o.proximoRetryEm <= :now)
            order by o.id asc
            """)
    List<SisbajudOperacao> findRetryCandidates(@Param("now") Instant now);
}
