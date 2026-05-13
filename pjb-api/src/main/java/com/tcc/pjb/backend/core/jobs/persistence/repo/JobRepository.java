package com.tcc.pjb.backend.core.jobs.persistence.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.core.jobs.domain.JobStatus;
import com.tcc.pjb.backend.core.jobs.domain.JobType;
import com.tcc.pjb.backend.core.jobs.persistence.entity.Job;

public interface JobRepository extends JpaRepository<Job, UUID> {

    Optional<Job> findByTypeAndIdempotencyKey(JobType type, String idempotencyKey);

    @Query("select j from Job j where (:inboxKey is null or j.inboxKey = :inboxKey) and (:status is null or j.status = :status)")
    Page<Job> list(@Param("inboxKey") String inboxKey, @Param("status") JobStatus status, Pageable pageable);

    List<Job> findTop100ByStatusOrderByCreatedAtAsc(JobStatus status);
}
