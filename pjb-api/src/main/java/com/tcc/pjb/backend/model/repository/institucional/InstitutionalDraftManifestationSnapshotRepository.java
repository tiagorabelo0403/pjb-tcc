package com.tcc.pjb.backend.model.repository.institucional;

import com.tcc.pjb.backend.model.entity.institucional.InstitutionalDraftManifestationSnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionalDraftManifestationSnapshotRepository extends JpaRepository<InstitutionalDraftManifestationSnapshot, Long> {
    Optional<InstitutionalDraftManifestationSnapshot> findByDraftId(String draftId);
    List<InstitutionalDraftManifestationSnapshot> findByExpedicaoUuidOrderByUpdatedAtAsc(String expedicaoUuid);
    List<InstitutionalDraftManifestationSnapshot> findByProcessoIdOrderByUpdatedAtAsc(Long processoId);
    List<InstitutionalDraftManifestationSnapshot> findAllByOrderByUpdatedAtAsc();
}
