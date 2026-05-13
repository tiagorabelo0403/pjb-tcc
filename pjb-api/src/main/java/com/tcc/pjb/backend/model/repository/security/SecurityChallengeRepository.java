package com.tcc.pjb.backend.model.repository.security;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.security.SecurityChallenge;

public interface SecurityChallengeRepository extends JpaRepository<SecurityChallenge, Long> {

    @Query("select c from SecurityChallenge c where c.id = :id")
    Optional<SecurityChallenge> findByIdSafe(@Param("id") Long id);

    @Modifying
    @Query("delete from SecurityChallenge c where c.usuario.id = :userId")
    int deleteAllByUser(@Param("userId") Long userId);
}
