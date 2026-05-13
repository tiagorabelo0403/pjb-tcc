package com.tcc.pjb.backend.model.repository.institucional;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalCompetenceRuleSnapshot;

public interface InstitutionalCompetenceRuleSnapshotRepository extends JpaRepository<InstitutionalCompetenceRuleSnapshot, Long> {
    Optional<InstitutionalCompetenceRuleSnapshot> findByRuleId(String ruleId);
    List<InstitutionalCompetenceRuleSnapshot> findAllByOrderByPrioridadeDescUpdatedAtDesc();
}
