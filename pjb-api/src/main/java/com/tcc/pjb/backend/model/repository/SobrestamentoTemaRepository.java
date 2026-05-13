package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.judicial.SobrestamentoTema;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SobrestamentoTemaRepository extends JpaRepository<SobrestamentoTema, Long> {

    List<SobrestamentoTema> findTop100ByTemaIdOrderBySobrestadoEmDesc(Long temaId);

    long countByTemaIdAndRetomadoEmIsNull(Long temaId);

    long countByTemaIdAndRetomadoEmIsNotNull(Long temaId);

    boolean existsByProcessoIdAndTemaId(Long processoId, Long temaId);

    List<SobrestamentoTema> findByTemaIdAndRetomadoEmIsNull(Long temaId);
}
