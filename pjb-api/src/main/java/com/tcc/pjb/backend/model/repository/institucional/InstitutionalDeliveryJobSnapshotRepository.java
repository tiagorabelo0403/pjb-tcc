package com.tcc.pjb.backend.model.repository.institucional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalDeliveryJobSnapshot;

public interface InstitutionalDeliveryJobSnapshotRepository extends JpaRepository<InstitutionalDeliveryJobSnapshot, Long> {
    Optional<InstitutionalDeliveryJobSnapshot> findByJobId(String jobId);
    List<InstitutionalDeliveryJobSnapshot> findByExpedicaoUuidOrderByUpdatedAtAsc(String expedicaoUuid);
    List<InstitutionalDeliveryJobSnapshot> findByProcessoIdOrderByUpdatedAtAsc(Long processoId);
    List<InstitutionalDeliveryJobSnapshot> findByUnidadeCodigoContainingIgnoreCaseOrderByUpdatedAtAsc(String unidadeFragment);
    List<InstitutionalDeliveryJobSnapshot> findByDestinatarioKindCodigoOrderByUpdatedAtAsc(String destinatarioKindCodigo);
    List<InstitutionalDeliveryJobSnapshot> findByUnidadeCodigoContainingIgnoreCaseAndDestinatarioKindCodigoOrderByUpdatedAtAsc(String unidadeFragment, String destinatarioKindCodigo);
    List<InstitutionalDeliveryJobSnapshot> findAllByOrderByUpdatedAtAsc();
    List<InstitutionalDeliveryJobSnapshot> findByStatusCodigoInOrderByUpdatedAtAsc(Collection<String> statusCodes);
    List<InstitutionalDeliveryJobSnapshot> findByStatusCodigoInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscUpdatedAtAsc(Collection<String> statusCodes, Instant nextAttemptAt);
}
