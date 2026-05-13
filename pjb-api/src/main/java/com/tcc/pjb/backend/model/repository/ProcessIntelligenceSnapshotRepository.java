package com.tcc.pjb.backend.model.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.ProcessIntelligenceSnapshot;

@Repository
public interface ProcessIntelligenceSnapshotRepository extends JpaRepository<ProcessIntelligenceSnapshot, Long> {
    Optional<ProcessIntelligenceSnapshot> findTopByProcessoIdOrderByDataCriacaoDesc(Long processoId);
}
