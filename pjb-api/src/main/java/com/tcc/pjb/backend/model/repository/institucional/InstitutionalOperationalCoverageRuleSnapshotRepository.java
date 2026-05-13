package com.tcc.pjb.backend.model.repository.institucional;

import com.tcc.pjb.backend.model.entity.institucional.InstitutionalOperationalCoverageRuleSnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionalOperationalCoverageRuleSnapshotRepository extends JpaRepository<InstitutionalOperationalCoverageRuleSnapshot, Long> {
    Optional<InstitutionalOperationalCoverageRuleSnapshot> findByRuleId(String ruleId);
    List<InstitutionalOperationalCoverageRuleSnapshot> findByUnidadeCodigoOrderByUpdatedAtAsc(String unidadeCodigo);
    List<InstitutionalOperationalCoverageRuleSnapshot> findByCaixaCodigoOrderByUpdatedAtAsc(String caixaCodigo);
    List<InstitutionalOperationalCoverageRuleSnapshot> findByCoberturaUsuarioIdOrderByUpdatedAtAsc(Long coberturaUsuarioId);
    List<InstitutionalOperationalCoverageRuleSnapshot> findAllByOrderByUpdatedAtAsc();
}
