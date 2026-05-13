package com.tcc.pjb.backend.model.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.Audiencia;

public interface AudienciaRepository extends JpaRepository<Audiencia, Long> {

    Optional<Audiencia> findTopByProcesso_IdOrderByDataHoraDesc(Long processoId);


    @Query(value = """
        SELECT DISTINCT ON (a.processo_id) a.*
        FROM tb_audiencia a
        WHERE a.processo_id = ANY(:processoIds)
          AND a.data_hora >= :from
          AND a.status IN ('AGENDADA', 'REDESIGNADA', 'ADIADA')
        ORDER BY a.processo_id, a.data_hora ASC
        """, nativeQuery = true)
    List<Audiencia> findNextUpcomingByProcessoIds(@Param("processoIds") long[] processoIds,
                                                 @Param("from") LocalDateTime from);

    @Query(value = """
        SELECT a.*
        FROM tb_audiencia a
        WHERE a.processo_id = ANY(:processoIds)
          AND a.data_hora >= :from
          AND a.data_hora <= :to
        ORDER BY a.data_hora ASC
        """, nativeQuery = true)
    List<Audiencia> findUpcomingByProcessoIds(@Param("processoIds") long[] processoIds,
                                             @Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);


    @Query(value = """
        SELECT a.id, a.processo_id, a.data_hora
        FROM tb_audiencia a
        WHERE a.processo_id = ANY(:processoIds)
          AND a.data_hora >= :from
          AND a.data_hora <= :to
        ORDER BY a.data_hora ASC
        """, nativeQuery = true)
    List<Object[]> findCalendarRowsByProcessoIds(@Param("processoIds") long[] processoIds,
                                                @Param("from") LocalDateTime from,
                                                @Param("to") LocalDateTime to);

    @Query(value = """
        SELECT a.*
        FROM tb_audiencia a
        INNER JOIN tb_processo p ON p.id = a.processo_id
        WHERE p.vara = :vara
          AND a.data_hora >= :from
          AND a.data_hora <= :to
          AND a.status NOT IN ('CANCELADA','ENCERRADA','FRUSTRADA')
        ORDER BY a.data_hora ASC
        """, nativeQuery = true)
    List<Audiencia> findAgendaPorVara(@Param("vara") String vara,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    @Query("SELECT a FROM Audiencia a WHERE a.processo.id = :processoId ORDER BY a.dataHora DESC")
    List<Audiencia> findByProcessoIdOrderByDataHoraDesc(@Param("processoId") Long processoId);

}

