package com.tcc.pjb.backend.model.repository.institucional;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalTimelineEventSnapshot;

public interface InstitutionalTimelineEventSnapshotRepository extends JpaRepository<InstitutionalTimelineEventSnapshot, Long> {
    List<InstitutionalTimelineEventSnapshot> findByExpedicaoUuidOrderByOccurredAtAsc(String expedicaoUuid);
}
