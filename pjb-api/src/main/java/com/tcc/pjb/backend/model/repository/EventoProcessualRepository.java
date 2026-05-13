package com.tcc.pjb.backend.model.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.EventoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusEvento;

@Repository
public interface EventoProcessualRepository extends JpaRepository<EventoProcessual, Long> {

    interface NextPendingDeadlineView {
        Long getProcessoId();
        String getTitulo();
        java.time.LocalDateTime getDataFim();
    }


    List<EventoProcessual> findByResponsavel_IdAndStatusOrderByDataFimAsc(
            Long responsavelId,
            StatusEvento status
    );

    List<EventoProcessual> findByProcesso_IdOrderByDataInicioAsc(Long processoId);

    List<EventoProcessual> findTop36ByProcesso_IdOrderByDataInicioDesc(Long processoId);

    List<EventoProcessual> findTop48ByProcesso_IdOrderByDataInicioDesc(Long processoId);

    
    @Query("""
        SELECT e
        FROM EventoProcessual e
        WHERE e.responsavel.id = :responsavelId
          AND e.dataInicio < :fim
          AND e.dataFim > :inicio
        ORDER BY e.dataInicio ASC
    """)
    List<EventoProcessual> findEventosNaAgenda(
            @Param("responsavelId") Long responsavelId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    
    @Query("""
        SELECT e
        FROM EventoProcessual e
        WHERE e.responsavel.id = :responsavelId
          AND e.dataInicio < :fim
          AND e.dataFim > :inicio
          AND (:excludeId IS NULL OR e.id <> :excludeId)
        ORDER BY e.dataInicio ASC
    """)
    List<EventoProcessual> findOverlapping(
            @Param("responsavelId") Long responsavelId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("excludeId") Long excludeId
    );

    @Query(value = """
        select distinct on (e.processo_id)
               e.processo_id as processoId,
               e.titulo as titulo,
               e.data_fim as dataFim
          from evento_processual e
         where e.responsavel_id = :responsavelId
           and e.processo_id in (:processoIds)
           and e.status = 'PENDENTE'
           and e.ativo = true
         order by e.processo_id, e.data_fim asc, e.id asc
        """, nativeQuery = true)
    List<NextPendingDeadlineView> findNextPendingDeadlines(@Param("responsavelId") Long responsavelId,
                                                           @Param("processoIds") List<Long> processoIds);

}
