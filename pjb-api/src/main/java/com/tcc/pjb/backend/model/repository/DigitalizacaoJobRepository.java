package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.digitalizacao.DigitalizacaoJob;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DigitalizacaoJobRepository extends JpaRepository<DigitalizacaoJob, Long> {

    List<DigitalizacaoJob> findTop100ByStatusOrderByCreatedAtAsc(String status);

    @Query("""
            select j from DigitalizacaoJob j
            where j.status = 'REVISAO_HUMANA'
            order by j.createdAt asc
            """)
    List<DigitalizacaoJob> findReviewQueue(org.springframework.data.domain.Pageable pageable);

    @Query("""
            select j from DigitalizacaoJob j
            where j.status = 'PROCESSANDO'
              and j.startedAt is not null
              and j.startedAt <= :cutoff
            order by j.startedAt asc
            """)
    List<DigitalizacaoJob> findStaleProcessingJobs(@Param("cutoff") Instant cutoff);
}
