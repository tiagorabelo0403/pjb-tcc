package com.tcc.pjb.backend.model.repository.security;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.security.TrustedDevice;

public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, Long> {

    Optional<TrustedDevice> findByCredentialId(String credentialId);

    @Query("select d from TrustedDevice d where d.usuario.id = :userId and d.revogadoEm is null order by d.atualizadoEm desc")
    List<TrustedDevice> findActiveByUser(@Param("userId") Long userId);

    @Query("select d from TrustedDevice d where d.id = :deviceId and d.usuario.id = :userId")
    Optional<TrustedDevice> findByIdAndUser(@Param("deviceId") Long deviceId, @Param("userId") Long userId);

    @Query("select d from TrustedDevice d where d.usuario.id = :userId and d.pendingChallengeId = :challengeId and d.revogadoEm is null")
    List<TrustedDevice> findPendingByUserAndChallengeId(@Param("userId") Long userId, @Param("challengeId") Long challengeId);

    @Modifying
    @Query("update TrustedDevice d set d.revogadoEm = :now where d.usuario.id = :userId and d.revogadoEm is null")
    int revokeAllByUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
