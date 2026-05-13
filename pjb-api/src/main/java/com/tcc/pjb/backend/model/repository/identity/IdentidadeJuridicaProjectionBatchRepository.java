package com.tcc.pjb.backend.model.repository.identity;

import com.tcc.pjb.backend.model.entity.identity.IdentidadeJuridicaProjectionBatchEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentidadeJuridicaProjectionBatchRepository extends JpaRepository<IdentidadeJuridicaProjectionBatchEntity, Long> {
    Optional<IdentidadeJuridicaProjectionBatchEntity> findTopByCorrelacaoIdOrderByCreatedAtDesc(String correlacaoId);
}
