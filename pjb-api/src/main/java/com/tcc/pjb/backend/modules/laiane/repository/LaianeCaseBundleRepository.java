package com.tcc.pjb.backend.modules.laiane.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeCaseBundle;
import com.tcc.pjb.backend.modules.laiane.model.LaianeCaseBundleStatus;

@Repository
public interface LaianeCaseBundleRepository extends JpaRepository<LaianeCaseBundle, Long> {

    Page<LaianeCaseBundle> findByAdvogado_IdOrderByCreatedAtDesc(Long advogadoId, Pageable pageable);

    Page<LaianeCaseBundle> findByAdvogado_IdAndStatusOrderByCreatedAtDesc(Long advogadoId, LaianeCaseBundleStatus status, Pageable pageable);
}
