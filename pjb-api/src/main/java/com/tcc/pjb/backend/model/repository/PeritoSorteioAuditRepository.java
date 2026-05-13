package com.tcc.pjb.backend.model.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.pericia.PeritoSorteioAudit;

public interface PeritoSorteioAuditRepository extends JpaRepository<PeritoSorteioAudit, Long> {

    List<PeritoSorteioAudit> findTop100ByProcesso_IdOrderByCreatedAtDesc(Long processoId);

    List<PeritoSorteioAudit> findTop100ByActor_IdOrderByCreatedAtDesc(Long actorId);
}
