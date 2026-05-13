package com.tcc.pjb.backend.model.repository.governance;

import com.tcc.pjb.backend.model.entity.governance.FonteSoberanaSnapshotEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FonteSoberanaSnapshotRepository extends JpaRepository<FonteSoberanaSnapshotEntity, Long> {
    Optional<FonteSoberanaSnapshotEntity> findByProcessoId(Long processoId);
}
