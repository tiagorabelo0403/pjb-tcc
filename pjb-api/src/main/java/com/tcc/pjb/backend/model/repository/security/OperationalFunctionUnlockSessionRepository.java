package com.tcc.pjb.backend.model.repository.security;

import com.tcc.pjb.backend.model.entity.security.OperationalFunctionUnlockSession;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OperationalFunctionUnlockSessionRepository extends JpaRepository<OperationalFunctionUnlockSession, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from OperationalFunctionUnlockSession s join fetch s.usuario join fetch s.credential where s.tokenHash = :tokenHash")
    Optional<OperationalFunctionUnlockSession> findLockedByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("delete from OperationalFunctionUnlockSession s where s.expiresAt < :cutoff or s.consumedAt is not null")
    int deleteExpiredOrConsumed(@Param("cutoff") LocalDateTime cutoff);
}
