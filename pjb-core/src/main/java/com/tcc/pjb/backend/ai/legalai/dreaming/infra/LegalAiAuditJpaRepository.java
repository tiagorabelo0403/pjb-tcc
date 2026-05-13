package com.tcc.pjb.backend.ai.legalai.dreaming.infra;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LegalAiAuditJpaRepository extends JpaRepository<LegalAiAuditJpaEntity, UUID> {

    List<LegalAiAuditJpaEntity> findByEntidadeTipoAndEntidadeId(String entidadeTipo, String entidadeId);

    List<LegalAiAuditJpaEntity> findByAcaoOrderByCriadoEmDesc(String acao);
}
