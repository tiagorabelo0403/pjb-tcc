package com.tcc.pjb.backend.modules.laiane.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeUserPreference;

@Repository
public interface LaianeUserPreferenceRepository extends JpaRepository<LaianeUserPreference, Long> {
    List<LaianeUserPreference> findByUsuario_IdOrderByUpdatedAtDescIdDesc(Long usuarioId, Pageable pageable);

    default Optional<LaianeUserPreference> findByUsuario_Id(Long usuarioId) {
        return findByUsuario_IdOrderByUpdatedAtDescIdDesc(usuarioId, PageRequest.of(0, 1)).stream().findFirst();
    }
}
