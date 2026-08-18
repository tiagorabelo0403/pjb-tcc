package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.competencia.Tribunal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TribunalRepository extends JpaRepository<Tribunal, Long> {
    Optional<Tribunal> findBySigla(String sigla);
}
