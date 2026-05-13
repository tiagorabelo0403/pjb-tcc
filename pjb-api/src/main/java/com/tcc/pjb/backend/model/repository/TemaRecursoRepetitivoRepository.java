package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.judicial.TemaRecursoRepetitivo;

public interface TemaRecursoRepetitivoRepository extends JpaRepository<TemaRecursoRepetitivo, Long> {

    Optional<TemaRecursoRepetitivo> findByCodigoIgnoreCase(String codigo);

    List<TemaRecursoRepetitivo> findTop100ByOrderByCreatedAtDesc();

    List<TemaRecursoRepetitivo> findTop100ByStatusOrderByCreatedAtDesc(String status);
}
