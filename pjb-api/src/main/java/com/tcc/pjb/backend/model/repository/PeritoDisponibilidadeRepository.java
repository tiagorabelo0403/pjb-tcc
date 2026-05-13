package com.tcc.pjb.backend.model.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.pericia.PeritoDisponibilidade;

public interface PeritoDisponibilidadeRepository extends JpaRepository<PeritoDisponibilidade, Long> {

    List<PeritoDisponibilidade> findTop100ByPerito_IdOrderByDataInicioDesc(Long peritoId);

    @Query("""
            select d from PeritoDisponibilidade d
            join fetch d.perito p
            where upper(d.especialidadeCodigo) = upper(:especialidadeCodigo)
              and (:comarca is null or d.comarca is null or upper(d.comarca) = upper(:comarca))
              and d.disponivel = true
              and d.dataInicio <= :data
              and d.dataFim >= :data
              and p.ativo = true
            order by d.dataInicio asc, d.id asc
            """)
    List<PeritoDisponibilidade> findDisponiveis(@Param("especialidadeCodigo") String especialidadeCodigo,
                                                @Param("comarca") String comarca,
                                                @Param("data") LocalDate data);
}
