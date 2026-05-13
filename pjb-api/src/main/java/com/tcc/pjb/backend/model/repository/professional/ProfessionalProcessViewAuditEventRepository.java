package com.tcc.pjb.backend.model.repository.professional;

import com.tcc.pjb.backend.model.entity.professional.ProfessionalProcessViewAuditEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessionalProcessViewAuditEventRepository extends JpaRepository<ProfessionalProcessViewAuditEvent, Long> {
    List<ProfessionalProcessViewAuditEvent> findTop20ByUsuarioIdOrderByAcessadoEmDesc(Long usuarioId);
    List<ProfessionalProcessViewAuditEvent> findTop30ByProcessoIdOrderByAcessadoEmDesc(Long processoId);
}
