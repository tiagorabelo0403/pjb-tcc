package com.tcc.pjb.backend.core.jobs.persistence.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.core.jobs.domain.JobItemStatus;
import com.tcc.pjb.backend.core.jobs.persistence.entity.JobItem;

public interface JobItemRepository extends JpaRepository<JobItem, UUID> {

    List<JobItem> findByJobIdOrderByCreatedAtAsc(UUID jobId);

    List<JobItem> findByJobIdAndStatusIn(UUID jobId, List<JobItemStatus> statuses);

    Optional<JobItem> findByJobIdAndItemKey(UUID jobId, String itemKey);
}
