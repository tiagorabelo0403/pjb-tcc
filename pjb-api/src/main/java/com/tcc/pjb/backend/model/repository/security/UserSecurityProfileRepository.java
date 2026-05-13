package com.tcc.pjb.backend.model.repository.security;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.security.UserSecurityProfile;

public interface UserSecurityProfileRepository extends JpaRepository<UserSecurityProfile, Long> {

    @Query("select p from UserSecurityProfile p where p.usuario.id = :userId")
    Optional<UserSecurityProfile> findByUserId(@Param("userId") Long userId);

    default Optional<UserSecurityProfile> findByUsuarioId(Long usuarioId) {
        return findByUserId(usuarioId);
    }
}
