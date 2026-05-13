package com.tcc.pjb.backend.model.repository.institucional;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalCatalogUnitSnapshot;

public interface InstitutionalCatalogUnitSnapshotRepository extends JpaRepository<InstitutionalCatalogUnitSnapshot, Long> {
    Optional<InstitutionalCatalogUnitSnapshot> findTopByCodigoUnidadeOrderByVigenciaInicioDesc(String codigoUnidade);
    List<InstitutionalCatalogUnitSnapshot> findByDestinatarioKindAndAtivaTrue(String destinatarioKind);
}
