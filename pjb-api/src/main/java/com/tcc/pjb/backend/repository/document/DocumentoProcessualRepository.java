package com.tcc.pjb.backend.repository.document;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;

public interface DocumentoProcessualRepository extends JpaRepository<DocumentoProcessual, UUID> {

    interface MonthCount {
        String getMonthKey();
        long getCnt();
    }

    interface ProcessoDocCount {
        Long getProcessoId();
        long getCnt();
    }

    @Query("select d from DocumentoProcessual d where d.processo.id = :processoId order by d.criadoEm desc")
    List<DocumentoProcessual> findByProcessoId(@Param("processoId") Long processoId);

    List<DocumentoProcessual> findTop18ByProcesso_IdOrderByCriadoEmDesc(Long processoId);

    long countByProcesso_Id(Long processoId);

    @Query("""
            select d from DocumentoProcessual d
            where d.processo.id = :processoId
              and d.sha256 = :sha256
            order by d.criadoEm desc, d.id desc
            """)
    List<DocumentoProcessual> findByProcessoIdAndSha256OrderByCriadoEmDescIdDesc(@Param("processoId") Long processoId,
                                                                                 @Param("sha256") String sha256,
                                                                                 Pageable pageable);

    default Optional<DocumentoProcessual> findFirstByProcesso_IdAndSha256(Long processoId, String sha256) {
        return findByProcessoIdAndSha256OrderByCriadoEmDescIdDesc(processoId, sha256, PageRequest.of(0, 1)).stream().findFirst();
    }


    List<DocumentoProcessual> findBySha256(String sha256);

    List<DocumentoProcessual> findBySha384(String sha384);

    @Query(
        value = "select d.processo_id as processoId, count(*) as cnt " +
            "from tb_documento_processual d " +
            "where d.processo_id in (:processoIds) " +
            "group by d.processo_id",
        nativeQuery = true
    )
    List<ProcessoDocCount> countDocsByProcessoIds(@Param("processoIds") List<Long> processoIds);

    @Query(
        value = "select to_char(date_trunc('month', d.criado_em), 'YYYY-MM') as monthKey, count(*) as cnt " +
            "from tb_documento_processual d " +
            "where d.processo_id in (:processoIds) and d.criado_em >= :fromDate " +
            "group by monthKey " +
            "order by monthKey",
        nativeQuery = true
    )
    List<MonthCount> countCreatedByMonthForProcessoIds(@Param("processoIds") List<Long> processoIds, @Param("fromDate") java.time.LocalDateTime fromDate);


    @Query("""
            select d from DocumentoProcessual d
            join fetch d.processo p
            where p.id = :processoId
            order by d.criadoEm desc
            """)
    List<DocumentoProcessual> findContextoByProcessoId(@Param("processoId") Long processoId, Pageable pageable);

    default List<DocumentoProcessual> findRecentContextoByProcessoId(Long processoId, int limit) {
        int safeLimit = Math.max(1, Math.min(100, limit));
        return findContextoByProcessoId(processoId, Pageable.ofSize(safeLimit));
    }

    @Query("""
            select d from DocumentoProcessual d
            where d.processo.id = :processoId
              and ((:sha256 is not null and d.sha256 = :sha256) or (:sha384 is not null and d.sha384 = :sha384))
            order by d.criadoEm desc
            """)
    List<DocumentoProcessual> findContextoCorrelatoByHash(@Param("processoId") Long processoId,
                                                          @Param("sha256") String sha256,
                                                          @Param("sha384") String sha384);

    @Query("select case when count(d) > 0 then true else false end from DocumentoProcessual d where d.processo.id = :processoId and d.sha256 = :sha256")
    boolean existsByProcessoIdAndSha256(@Param("processoId") Long processoId, @Param("sha256") String sha256);
}
