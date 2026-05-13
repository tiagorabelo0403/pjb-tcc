package com.tcc.pjb.backend.model.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.NegotiationRoundSnapshot;

@Repository
public interface NegotiationRoundSnapshotRepository extends JpaRepository<NegotiationRoundSnapshot, Long> {
    Optional<NegotiationRoundSnapshot> findTopByProcessoIdOrderByDataCriacaoDesc(Long processoId);
}
