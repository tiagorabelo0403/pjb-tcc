package com.tcc.pjb.backend.model.repository.institucional;

import com.tcc.pjb.backend.model.entity.institucional.InstitutionalNominationSnapshot;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionalNominationSnapshotRepository extends JpaRepository<InstitutionalNominationSnapshot, Long> {
    Optional<InstitutionalNominationSnapshot> findByNominationId(String nominationId);
    List<InstitutionalNominationSnapshot> findByNominatedUserIdOrderByUpdatedAtAsc(Long nominatedUserId);
    List<InstitutionalNominationSnapshot> findByAffiliationIdOrderByUpdatedAtAsc(String affiliationId);
    List<InstitutionalNominationSnapshot> findByAffiliationIdInOrderByUpdatedAtAsc(Collection<String> affiliationIds);
    List<InstitutionalNominationSnapshot> findByUnidadeCodigoOrderByUpdatedAtAsc(String unidadeCodigo);
    List<InstitutionalNominationSnapshot> findAllByOrderByUpdatedAtAsc();
}
