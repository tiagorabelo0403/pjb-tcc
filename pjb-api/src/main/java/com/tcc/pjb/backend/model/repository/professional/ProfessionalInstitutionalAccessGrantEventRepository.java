package com.tcc.pjb.backend.model.repository.professional;

import com.tcc.pjb.backend.model.entity.professional.ProfessionalInstitutionalAccessGrantEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessionalInstitutionalAccessGrantEventRepository extends JpaRepository<ProfessionalInstitutionalAccessGrantEvent, Long> {
    List<ProfessionalInstitutionalAccessGrantEvent> findTop100ByGrant_IdOrderByCreatedAtDesc(Long grantId);
    List<ProfessionalInstitutionalAccessGrantEvent> findTop200ByGrant_Processo_IdOrderByCreatedAtDesc(Long processoId);
}
