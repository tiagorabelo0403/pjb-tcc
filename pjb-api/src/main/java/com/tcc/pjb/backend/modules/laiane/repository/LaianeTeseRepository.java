package com.tcc.pjb.backend.modules.laiane.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeTese;

public interface LaianeTeseRepository extends JpaRepository<LaianeTese, Long> {
    Page<LaianeTese> findByAdvogado_Id(Long advogadoId, Pageable pageable);
}
