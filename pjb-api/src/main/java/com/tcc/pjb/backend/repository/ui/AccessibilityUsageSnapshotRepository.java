package com.tcc.pjb.backend.repository.ui;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.ui.AccessibilityUsageSnapshot;

public interface AccessibilityUsageSnapshotRepository extends JpaRepository<AccessibilityUsageSnapshot, Long> {

  @Query("""
      select s from AccessibilityUsageSnapshot s
      where s.usuarioId = :usuarioId
      order by s.observedAt desc
      """)
  Optional<AccessibilityUsageSnapshot> findLatest(Long usuarioId);

  @Modifying
  @Query("delete from AccessibilityUsageSnapshot s where s.observedAt < :cutoff")
  int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
