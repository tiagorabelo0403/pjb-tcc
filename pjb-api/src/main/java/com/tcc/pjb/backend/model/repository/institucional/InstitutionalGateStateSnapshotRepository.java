package com.tcc.pjb.backend.model.repository.institucional;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalGateStateSnapshot;

public interface InstitutionalGateStateSnapshotRepository extends JpaRepository<InstitutionalGateStateSnapshot, Long> {
    java.util.List<InstitutionalGateStateSnapshot> findAllByOrderByUpdatedAtAsc();
    Optional<InstitutionalGateStateSnapshot> findByExpedicaoUuid(String expedicaoUuid);
    List<InstitutionalGateStateSnapshot> findByProcessoIdOrderByUpdatedAtAsc(Long processoId);
    List<InstitutionalGateStateSnapshot> findByGateCodeContainingIgnoreCaseOrderByUpdatedAtAsc(String gateCodeFragment);
}
