package com.tcc.pjb.backend.model.repository.institucional;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalCatalogGovernanceSnapshot;

public interface InstitutionalCatalogGovernanceSnapshotRepository extends JpaRepository<InstitutionalCatalogGovernanceSnapshot, Long> {
    Optional<InstitutionalCatalogGovernanceSnapshot> findByGovernanceId(String governanceId);
    List<InstitutionalCatalogGovernanceSnapshot> findByUnidadeCodigoOrderByVigenciaInicioDesc(String unidadeCodigo);
    List<InstitutionalCatalogGovernanceSnapshot> findAllByOrderByUpdatedAtDesc();
}
