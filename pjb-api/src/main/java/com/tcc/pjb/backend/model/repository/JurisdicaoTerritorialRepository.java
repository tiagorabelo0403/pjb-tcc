package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.competencia.JurisdicaoTerritorial;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JurisdicaoTerritorialRepository extends JpaRepository<JurisdicaoTerritorial, Long> {

    @Query("""
            SELECT j FROM JurisdicaoTerritorial j
            WHERE j.municipioIbge = :municipioIbge
              AND j.tipoJustica = :tipoJustica
              AND j.vigenciaInicio <= :referencia
              AND (j.vigenciaFim IS NULL OR j.vigenciaFim > :referencia)
            """)
    Optional<JurisdicaoTerritorial> findVigente(@Param("municipioIbge") String municipioIbge,
            @Param("tipoJustica") String tipoJustica, @Param("referencia") LocalDate referencia);
}
