package com.tcc.pjb.backend.model.repository;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.identity.GovBrLinkState;

public interface GovBrLinkStateRepository extends JpaRepository<GovBrLinkState, UUID> {

  @Modifying
  @Query("delete from GovBrLinkState s where s.expiresAt < :now")
  int deleteExpired(@Param("now") Instant now);
}
