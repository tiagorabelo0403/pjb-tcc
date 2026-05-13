package com.tcc.pjb.backend.ai.legalai.dreaming.infra;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MemoryCandidateReviewJpaRepository extends JpaRepository<MemoryCandidateReviewJpaEntity, UUID> {

    List<MemoryCandidateReviewJpaEntity> findByStatusOrderByCriadoEmAsc(String status);

    List<MemoryCandidateReviewJpaEntity> findByModuloOrigemOrderByCriadoEmDesc(String moduloOrigem);
}
