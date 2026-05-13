package com.tcc.pjb.backend.model.repository.security;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.security.PasskeySession;

public interface PasskeySessionRepository extends JpaRepository<PasskeySession, Long> {

    @Query("select s from PasskeySession s where s.tokenHash = :hash and s.revogadoEm is null")
    Optional<PasskeySession> findActiveByTokenHash(@Param("hash") String hash);

    @Modifying
    @Query("delete from PasskeySession s where s.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);

    @Modifying
    @Query("update PasskeySession s set s.revogadoEm = :now where s.usuario.id = :userId and s.revogadoEm is null")
    int revokeAllByUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
