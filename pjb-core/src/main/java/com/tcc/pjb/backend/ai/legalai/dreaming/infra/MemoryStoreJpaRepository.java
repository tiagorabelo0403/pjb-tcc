package com.tcc.pjb.backend.ai.legalai.dreaming.infra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemoryStoreJpaRepository extends JpaRepository<MemoryStoreJpaEntity, UUID> {

    Optional<MemoryStoreJpaEntity> findByAnthropicStoreId(String anthropicStoreId);

    @Query("SELECT m FROM MemoryStoreJpaEntity m WHERE m.arquivadoEm IS NULL")
    List<MemoryStoreJpaEntity> findAllAtivos();

    List<MemoryStoreJpaEntity> findByModuloOrigemAndArquivadoEmIsNull(String moduloOrigem);
}
