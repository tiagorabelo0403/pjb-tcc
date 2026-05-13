package com.tcc.pjb.backend.model.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;

public interface MovimentacaoProcessualRepository extends JpaRepository<MovimentacaoProcessual, Long> {

    interface MonthCount {
        String getMonthKey();
        long getCnt();
    }

    List<MovimentacaoProcessual> findTop200ByProcesso_IdOrderByDataMovimentacaoDesc(Long processoId);

    List<MovimentacaoProcessual> findTop60ByProcesso_IdOrderByDataMovimentacaoDesc(Long processoId);

    List<MovimentacaoProcessual> findTop80ByProcesso_IdOrderByDataMovimentacaoDesc(Long processoId);

    Optional<MovimentacaoProcessual> findTop1ByProcesso_IdOrderByDataMovimentacaoDesc(Long processoId);

    List<MovimentacaoProcessual> findByProcesso_IdAndDataMovimentacaoAfterOrderByDataMovimentacaoAsc(Long processoId, Instant fromInstant);

    @Query(
        value = "select distinct on (m.processo_id) m.* " +
            "from tb_movimentacao_processual m " +
            "where m.processo_id in (:processoIds) " +
            "order by m.processo_id, m.data_movimentacao desc",
        nativeQuery = true
    )
    List<MovimentacaoProcessual> findLatestByProcessoIds(@Param("processoIds") List<Long> processoIds);

    @Query(
        value = "select to_char(date_trunc('month', m.data_movimentacao), 'YYYY-MM') as monthKey, count(*) as cnt " +
            "from tb_movimentacao_processual m " +
            "where m.processo_id in (:processoIds) and m.data_movimentacao >= :fromInstant " +
            "group by monthKey " +
            "order by monthKey",
        nativeQuery = true
    )
    List<MonthCount> countByMonthForProcessoIds(@Param("processoIds") List<Long> processoIds, @Param("fromInstant") Instant fromInstant);
}
