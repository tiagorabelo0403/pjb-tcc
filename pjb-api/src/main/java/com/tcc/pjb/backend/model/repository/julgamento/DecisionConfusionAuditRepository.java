package com.tcc.pjb.backend.model.repository.julgamento;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.julgamento.DecisionConfusionAudit;

public interface DecisionConfusionAuditRepository extends JpaRepository<DecisionConfusionAudit, Long> {

    List<DecisionConfusionAudit> findTop30ByUsuario_IdOrderByCreatedAtDesc(Long usuarioId);

    List<DecisionConfusionAudit> findTop30ByProcesso_IdOrderByCreatedAtDesc(Long processoId);
}
