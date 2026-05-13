package com.tcc.pjb.backend.model.repository.security;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.security.StrongAuthUsage;

public interface StrongAuthUsageRepository extends JpaRepository<StrongAuthUsage, Long> {

    @Query("select u from StrongAuthUsage u where u.sessionId = :sessionId and u.actionHash = :actionHash")
    Optional<StrongAuthUsage> findBySessionAndAction(@Param("sessionId") Long sessionId,
                                                     @Param("actionHash") String actionHash);
}
