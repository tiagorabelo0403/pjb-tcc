package com.tcc.pjb.backend.model.repository.security;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.security.WebAuthnSession;

public interface WebAuthnSessionRepository extends JpaRepository<WebAuthnSession, Long> {

    @Query("select s from WebAuthnSession s where s.id = :id")
    Optional<WebAuthnSession> findByIdSafe(@Param("id") Long id);

    @Modifying
    @Query("delete from WebAuthnSession s where s.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from WebAuthnSession s where s.usuario.id = :userId")
    int deleteAllByUser(@Param("userId") Long userId);
}
