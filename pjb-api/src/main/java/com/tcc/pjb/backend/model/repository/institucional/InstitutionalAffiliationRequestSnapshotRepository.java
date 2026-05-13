package com.tcc.pjb.backend.model.repository.institucional;

import com.tcc.pjb.backend.model.entity.institucional.InstitutionalAffiliationRequestSnapshot;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InstitutionalAffiliationRequestSnapshotRepository extends JpaRepository<InstitutionalAffiliationRequestSnapshot, Long> {
    Optional<InstitutionalAffiliationRequestSnapshot> findByRequestId(String requestId);
    List<InstitutionalAffiliationRequestSnapshot> findByUnidadeCodigoOrderByUpdatedAtAsc(String unidadeCodigo);
    List<InstitutionalAffiliationRequestSnapshot> findByOrganizationScopeOrderByUpdatedAtAsc(String organizationScope);
    List<InstitutionalAffiliationRequestSnapshot> findByRepresentanteUsuarioIdOrderByUpdatedAtDesc(Long representanteUsuarioId);
    List<InstitutionalAffiliationRequestSnapshot> findByMaterializedAffiliationIdOrderByUpdatedAtDesc(String materializedAffiliationId);
    List<InstitutionalAffiliationRequestSnapshot> findByMaterializedAffiliationIdInOrderByUpdatedAtDesc(Collection<String> materializedAffiliationIds);
    List<InstitutionalAffiliationRequestSnapshot> findByStatusCodigoInOrderByUpdatedAtAsc(Collection<String> statusCodigos);
    @Query("select s from InstitutionalAffiliationRequestSnapshot s where s.materializedAffiliationId is null or s.materializedAffiliationId = '' order by s.updatedAt asc")
    List<InstitutionalAffiliationRequestSnapshot> findWithoutMaterializedAffiliationOrderByUpdatedAtAsc();
    List<InstitutionalAffiliationRequestSnapshot> findAllByOrderByUpdatedAtAsc();
}
