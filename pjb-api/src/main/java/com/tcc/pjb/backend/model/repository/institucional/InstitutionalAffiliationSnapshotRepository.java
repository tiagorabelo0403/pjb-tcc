package com.tcc.pjb.backend.model.repository.institucional;

import com.tcc.pjb.backend.model.entity.institucional.InstitutionalAffiliationSnapshot;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionalAffiliationSnapshotRepository extends JpaRepository<InstitutionalAffiliationSnapshot, Long> {
    Optional<InstitutionalAffiliationSnapshot> findByAffiliationId(String affiliationId);
    List<InstitutionalAffiliationSnapshot> findByAffiliationIdInOrderByUpdatedAtAsc(Collection<String> affiliationIds);
    List<InstitutionalAffiliationSnapshot> findByUnidadeCodigoOrderByUpdatedAtAsc(String unidadeCodigo);
    List<InstitutionalAffiliationSnapshot> findByOrganizationScopeOrderByUpdatedAtAsc(String organizationScope);
    List<InstitutionalAffiliationSnapshot> findByStatusCodigoInOrderByUpdatedAtAsc(Collection<String> statusCodes);
    List<InstitutionalAffiliationSnapshot> findByOrganizationScopeAndStatusCodigoInOrderByUpdatedAtAsc(String organizationScope, Collection<String> statusCodes);
    List<InstitutionalAffiliationSnapshot> findAllByOrderByUpdatedAtAsc();
}
