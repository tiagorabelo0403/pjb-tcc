package com.tcc.pjb.backend.model.repository.institucional;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalDeliveryAttemptSnapshot;

public interface InstitutionalDeliveryAttemptSnapshotRepository extends JpaRepository<InstitutionalDeliveryAttemptSnapshot, Long> {
    List<InstitutionalDeliveryAttemptSnapshot> findByJobIdOrderByAttemptNumberAsc(String jobId);
    List<InstitutionalDeliveryAttemptSnapshot> findAllByOrderByCreatedAtAsc();
}
