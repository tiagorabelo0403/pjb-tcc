package com.tcc.pjb.backend.model.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.competencia.UnidadeJudiciariaCompetencia;

@Repository
public interface UnidadeJudiciariaCompetenciaRepository extends JpaRepository<UnidadeJudiciariaCompetencia, Long> {

    Optional<UnidadeJudiciariaCompetencia> findByCodigo(String codigo);

    java.util.List<UnidadeJudiciariaCompetencia> findAllByTribunalCodigo(String tribunalCodigo);

    java.util.List<UnidadeJudiciariaCompetencia> findAllByTribunalCodigoAndComarcaIgnoreCase(String tribunalCodigo, String comarca);
}
