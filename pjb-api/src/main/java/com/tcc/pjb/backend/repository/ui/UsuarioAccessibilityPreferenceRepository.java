package com.tcc.pjb.backend.repository.ui;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.ui.UsuarioAccessibilityPreference;

public interface UsuarioAccessibilityPreferenceRepository extends JpaRepository<UsuarioAccessibilityPreference, Long> {
  List<UsuarioAccessibilityPreference> findByUsuarioIdOrderByUpdatedAtDescIdDesc(Long usuarioId, Pageable pageable);

  default Optional<UsuarioAccessibilityPreference> findByUsuarioId(Long usuarioId) {
    return findByUsuarioIdOrderByUpdatedAtDescIdDesc(usuarioId, PageRequest.of(0, 1)).stream().findFirst();
  }
}
