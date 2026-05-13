package com.tcc.pjb.backend.model.repository.institucional;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalExternalDispatchSnapshot;

public interface InstitutionalExternalDispatchSnapshotRepository extends JpaRepository<InstitutionalExternalDispatchSnapshot, Long> {
    Optional<InstitutionalExternalDispatchSnapshot> findByDispatchId(String dispatchId);
    List<InstitutionalExternalDispatchSnapshot> findByExpedicaoUuidOrderByUpdatedAtAsc(String expedicaoUuid);
    List<InstitutionalExternalDispatchSnapshot> findByProcessoIdOrderByUpdatedAtAsc(Long processoId);
    List<InstitutionalExternalDispatchSnapshot> findByUnidadeCodigoContainingIgnoreCaseOrderByUpdatedAtAsc(String unidadeFragment);
    List<InstitutionalExternalDispatchSnapshot> findByDestinatarioKindCodigoOrderByUpdatedAtAsc(String destinatarioKindCodigo);
    List<InstitutionalExternalDispatchSnapshot> findByUnidadeCodigoContainingIgnoreCaseAndDestinatarioKindCodigoOrderByUpdatedAtAsc(String unidadeFragment, String destinatarioKindCodigo);
    List<InstitutionalExternalDispatchSnapshot> findAllByOrderByUpdatedAtAsc();
}
