package com.tcc.pjb.backend.model.repository.recursalmesh;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalTransitionLedgerEntry;

public interface RecursalTransitionLedgerRepository extends JpaRepository<RecursalTransitionLedgerEntry, Long> {
    List<RecursalTransitionLedgerEntry> findTop100ByRecursoIdOrderByToRevisionDesc(String recursoId);
    Optional<RecursalTransitionLedgerEntry> findTopByRecursoIdAndCommandIdOrderByToRevisionDesc(String recursoId, String commandId);
}
