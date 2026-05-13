package com.tcc.pjb.backend.ai.legalai.dreaming.infra;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MemoryEntryJpaRepository extends JpaRepository<MemoryEntryJpaEntity, UUID> {

    List<MemoryEntryJpaEntity> findByStoreIdAndAtivoTrue(UUID storeId);
}
