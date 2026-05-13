package com.tcc.pjb.backend.model.repository.recursalmesh;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalProcessIntegrationState;

public interface RecursalProcessIntegrationStateRepository extends JpaRepository<RecursalProcessIntegrationState, String> {
    List<RecursalProcessIntegrationState> findTop50ByProcesso_IdOrderByUpdatedAtDesc(Long processoId);
    List<RecursalProcessIntegrationState> findTop200ByProcesso_IdOrderByUpdatedAtDesc(Long processoId);
    List<RecursalProcessIntegrationState> findTop200ByOrderByUpdatedAtDesc();
    Page<RecursalProcessIntegrationState> findByProcesso_IdIn(List<Long> processoIds, Pageable pageable);

    @Query("""
            select r
            from RecursalProcessIntegrationState r
            where r.updatedAt is not null
              and (
                    :updatedAt is null
                    or r.updatedAt > :updatedAt
                    or (r.updatedAt = :updatedAt and r.recursoId > :recursoId)
                  )
            order by r.updatedAt asc, r.recursoId asc
            """)
    List<RecursalProcessIntegrationState> findNextForReindex(@Param("updatedAt") Instant updatedAt,
                                                             @Param("recursoId") String recursoId,
                                                             Pageable pageable);
}
