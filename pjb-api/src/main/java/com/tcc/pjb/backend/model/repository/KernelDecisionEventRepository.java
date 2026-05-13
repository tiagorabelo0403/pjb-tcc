package com.tcc.pjb.backend.model.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.KernelDecisionEvent;

@Repository
public interface KernelDecisionEventRepository extends JpaRepository<KernelDecisionEvent, Long> {
    List<KernelDecisionEvent> findTop30ByProcessoIdOrderByDataCriacaoDesc(Long processoId);
    long countByProcessoId(Long processoId);
    long countByProcessoIdAndReleaseAllowedFalse(Long processoId);
    long countByProcessoIdAndApprovalRequiredTrue(Long processoId);
    long countByProcessoIdAndInternalDraftRequiredTrue(Long processoId);
    long countByProcessoIdAndDataCriacaoAfter(Long processoId, LocalDateTime dataCriacao);
}
