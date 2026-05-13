package com.tcc.pjb.backend.model.repository.institucional;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalDeliveryProofSnapshot;

public interface InstitutionalDeliveryProofSnapshotRepository extends JpaRepository<InstitutionalDeliveryProofSnapshot, Long> {
    List<InstitutionalDeliveryProofSnapshot> findByExpedicaoUuidOrderByCreatedAtAsc(String expedicaoUuid);
}
