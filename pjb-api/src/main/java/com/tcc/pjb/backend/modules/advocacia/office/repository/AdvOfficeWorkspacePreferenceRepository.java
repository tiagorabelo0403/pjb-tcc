package com.tcc.pjb.backend.modules.advocacia.office.repository;

import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeWorkspacePreference;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvOfficeWorkspacePreferenceRepository extends JpaRepository<AdvOfficeWorkspacePreference, Long> {

    Optional<AdvOfficeWorkspacePreference> findByUsuarioId(Long usuarioId);
}
