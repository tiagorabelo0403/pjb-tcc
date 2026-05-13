package com.tcc.pjb.backend.model.repository.security;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.security.PerfilBehaviorBaseline;

public interface PerfilBehaviorBaselineRepository extends JpaRepository<PerfilBehaviorBaseline, Long> {

    Optional<PerfilBehaviorBaseline> findByTipoUsuarioAndAtivoTrue(TipoUsuario tipoUsuario);
}
