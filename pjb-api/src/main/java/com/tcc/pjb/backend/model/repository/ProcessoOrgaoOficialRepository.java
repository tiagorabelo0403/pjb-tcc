package com.tcc.pjb.backend.model.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.orgao.OrgaoOficialTipo;
import com.tcc.pjb.backend.model.entity.orgao.ProcessoOrgaoOficial;

public interface ProcessoOrgaoOficialRepository extends JpaRepository<ProcessoOrgaoOficial, Long> {

    boolean existsByProcesso_IdAndOrgaoTipo(Long processoId, OrgaoOficialTipo orgaoTipo);

    List<ProcessoOrgaoOficial> findTop50ByProcesso_IdOrderByCreatedAtDesc(Long processoId);
}
