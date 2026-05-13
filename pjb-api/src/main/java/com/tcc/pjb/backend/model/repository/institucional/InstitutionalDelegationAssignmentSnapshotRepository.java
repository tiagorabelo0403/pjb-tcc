package com.tcc.pjb.backend.model.repository.institucional;

import com.tcc.pjb.backend.model.entity.institucional.InstitutionalDelegationAssignmentSnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionalDelegationAssignmentSnapshotRepository extends JpaRepository<InstitutionalDelegationAssignmentSnapshot, Long> {
    Optional<InstitutionalDelegationAssignmentSnapshot> findByAssignmentId(String assignmentId);
    List<InstitutionalDelegationAssignmentSnapshot> findByExpedicaoUuidOrderByUpdatedAtAsc(String expedicaoUuid);
    List<InstitutionalDelegationAssignmentSnapshot> findByProcessoIdOrderByUpdatedAtAsc(Long processoId);
    List<InstitutionalDelegationAssignmentSnapshot> findByDelegadoUsuarioIdOrderByUpdatedAtAsc(Long delegadoUsuarioId);
    List<InstitutionalDelegationAssignmentSnapshot> findAllByOrderByUpdatedAtAsc();
}
