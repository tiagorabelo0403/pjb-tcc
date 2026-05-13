package com.tcc.pjb.backend.core.backfill.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BackfillRunRepository extends JpaRepository<BackfillRun, UUID> {

    @Query("select r from BackfillRun r where r.type = :type and (:inboxKey is null or r.inboxKey = :inboxKey) order by r.createdAt desc")
    Optional<BackfillRun> findLatest(@Param("type") String type, @Param("inboxKey") String inboxKey);
}
