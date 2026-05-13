package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.digitalizacao.DigitalizacaoPagina;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DigitalizacaoPaginaRepository extends JpaRepository<DigitalizacaoPagina, Long> {
    List<DigitalizacaoPagina> findByJobIdOrderByNumeroPaginaAsc(Long jobId);
    long countByJobIdAndRevisadoFalse(Long jobId);
}
