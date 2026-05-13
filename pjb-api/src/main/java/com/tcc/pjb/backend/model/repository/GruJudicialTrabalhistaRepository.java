package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.financeiro.GruJudicialTrabalhista;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GruJudicialTrabalhistaRepository extends JpaRepository<GruJudicialTrabalhista, Long> {
    List<GruJudicialTrabalhista> findByProcessoIdOrderByCreatedAtDesc(Long processoId);
}
