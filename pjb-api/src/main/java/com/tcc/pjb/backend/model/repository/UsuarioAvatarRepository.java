package com.tcc.pjb.backend.model.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.identity.UsuarioAvatar;

public interface UsuarioAvatarRepository extends JpaRepository<UsuarioAvatar, Long> {
  Optional<UsuarioAvatar> findByUsuarioId(Long usuarioId);
}
