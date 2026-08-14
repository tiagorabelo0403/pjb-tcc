package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComarcaRepository extends JpaRepository<Comarca, Long> {
    Optional<Comarca> findByNomeIgnoreCaseAndUf(String nome, String uf);
    List<Comarca> findAllByNomeIgnoreCase(String nome);
}
