package com.tcc.pjb.backend.ai.legalai.dreaming.infra;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DreamJpaRepository extends JpaRepository<DreamJpaEntity, UUID> {

    Optional<DreamJpaEntity> findByAnthropicDreamId(String anthropicDreamId);

    List<DreamJpaEntity> findByStatus(String status);
}
