package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.infra.ProcessualReadModelRecompositionJob;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessualReadModelRecompositionJobRepository extends JpaRepository<ProcessualReadModelRecompositionJob, Long> {

    List<ProcessualReadModelRecompositionJob> findTop20ByOrderByUpdatedAtDesc();

    List<ProcessualReadModelRecompositionJob> findTop50ByStatusInOrderByCreatedAtDesc(Collection<String> statuses);

    List<ProcessualReadModelRecompositionJob> findByStatusAndNotBeforeAtLessThanEqualOrderByCreatedAtAsc(String status,
                                                                                                          Instant notBeforeAt,
                                                                                                          Pageable pageable);

    long countByStatusIgnoreCase(String status);
}
