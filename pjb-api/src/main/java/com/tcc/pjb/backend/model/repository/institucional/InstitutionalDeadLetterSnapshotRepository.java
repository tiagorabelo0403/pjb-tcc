package com.tcc.pjb.backend.model.repository.institucional;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalDeadLetterSnapshot;

public interface InstitutionalDeadLetterSnapshotRepository extends JpaRepository<InstitutionalDeadLetterSnapshot, Long> {
    List<InstitutionalDeadLetterSnapshot> findByProcessoIdOrderByCreatedAtAsc(Long processoId);
    List<InstitutionalDeadLetterSnapshot> findByExpedicaoUuidOrderByCreatedAtAsc(String expedicaoUuid);
    List<InstitutionalDeadLetterSnapshot> findAllByOrderByCreatedAtAsc();
}
