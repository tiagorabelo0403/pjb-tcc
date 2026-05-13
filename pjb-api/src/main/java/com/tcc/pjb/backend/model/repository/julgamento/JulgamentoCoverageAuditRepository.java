package com.tcc.pjb.backend.model.repository.julgamento;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoCoverageAudit;

public interface JulgamentoCoverageAuditRepository extends JpaRepository<JulgamentoCoverageAudit, Long> {

    List<JulgamentoCoverageAudit> findTop50ByProcesso_IdOrderByCreatedAtDesc(Long processoId);
}
