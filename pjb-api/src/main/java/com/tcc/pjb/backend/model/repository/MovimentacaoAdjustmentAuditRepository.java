package com.tcc.pjb.backend.model.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoAdjustmentAudit;

public interface MovimentacaoAdjustmentAuditRepository extends JpaRepository<MovimentacaoAdjustmentAudit, Long> {

    List<MovimentacaoAdjustmentAudit> findTop50ByProcesso_IdOrderByCreatedAtDesc(Long processoId);
}
