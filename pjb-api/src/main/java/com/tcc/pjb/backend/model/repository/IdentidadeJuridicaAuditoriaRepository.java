package com.tcc.pjb.backend.model.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.identity.IdentidadeJuridicaAuditoria;

@Repository
public interface IdentidadeJuridicaAuditoriaRepository extends JpaRepository<IdentidadeJuridicaAuditoria, Long> {

    List<IdentidadeJuridicaAuditoria> findTop50ByIdentidade_IdOrderByCriadoEmDesc(java.util.UUID identidadeId);
}
