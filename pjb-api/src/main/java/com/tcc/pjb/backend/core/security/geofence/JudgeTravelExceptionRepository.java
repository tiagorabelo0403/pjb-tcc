package com.tcc.pjb.backend.core.security.geofence;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JudgeTravelExceptionRepository extends JpaRepository<JudgeTravelException, Long> {

    @Query("SELECT COUNT(e) > 0 FROM JudgeTravelException e WHERE e.usuarioId = :usuarioId "
            + "AND (e.ufOuPaisDestino = :pais OR e.ufOuPaisDestino = :uf) "
            + "AND :hoje BETWEEN e.dataInicio AND e.dataFim")
    boolean existeExcecaoAtivaParaDestino(@Param("usuarioId") Long usuarioId,
                                           @Param("pais") String pais,
                                           @Param("uf") String uf,
                                           @Param("hoje") LocalDate hoje);
}
