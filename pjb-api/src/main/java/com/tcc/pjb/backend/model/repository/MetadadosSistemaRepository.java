package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.MetadadosSistema;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetadadosSistemaRepository extends JpaRepository<MetadadosSistema, Long> {
    Optional<MetadadosSistema> findByChaveIgnoreCase(String chave);
}
