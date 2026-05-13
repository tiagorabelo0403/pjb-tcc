package com.tcc.pjb.backend.model.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.painel.PainelSerieTemporalDiaria;

public interface PainelSerieTemporalDiariaRepository extends JpaRepository<PainelSerieTemporalDiaria, Long> {

    Optional<PainelSerieTemporalDiaria> findByChaveSerie(String chaveSerie);

    List<PainelSerieTemporalDiaria> findAllByDataReferenciaGreaterThanEqualOrderByDataReferenciaAsc(LocalDate dataReferencia);
}
