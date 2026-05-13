package com.tcc.pjb.backend.model.repository.professional;

import com.tcc.pjb.backend.model.entity.professional.ProfessionalAccessGrantTemplate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessionalAccessGrantTemplateRepository extends JpaRepository<ProfessionalAccessGrantTemplate, Long> {

    List<ProfessionalAccessGrantTemplate> findByAtivoTrueOrderBySequenceOrderAscIdAsc();

    Optional<ProfessionalAccessGrantTemplate> findByTemplateCodeAndAtivoTrue(String templateCode);
}
