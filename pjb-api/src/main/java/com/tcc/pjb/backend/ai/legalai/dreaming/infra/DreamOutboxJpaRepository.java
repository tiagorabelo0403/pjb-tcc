package com.tcc.pjb.backend.ai.legalai.dreaming.infra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DreamOutboxJpaRepository extends JpaRepository<DreamOutboxJpaEntity, UUID> {

    List<DreamOutboxJpaEntity> findByProcessadoFalseOrderByCriadoEmAsc();

    @Modifying
    @Query("UPDATE DreamOutboxJpaEntity o SET o.processado = true, o.processadoEm = :agora WHERE o.id = :id")
    void marcarProcessado(UUID id, Instant agora);
}
