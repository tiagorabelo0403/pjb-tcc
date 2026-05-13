package com.tcc.pjb.backend.model.repository.julgamento;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;
import com.tcc.pjb.backend.model.entity.julgamento.enums.StatusJulgamentoColegiado;

public interface JulgamentoColegiadoRepository extends JpaRepository<JulgamentoColegiado, Long> {

  @Query("select j from JulgamentoColegiado j where j.processo.id = :pid order by coalesce(j.pautaDataHora, j.createdAt) desc")
  List<JulgamentoColegiado> findByProcessoId(@Param("pid") Long processoId);

  @Query("select j from JulgamentoColegiado j join fetch j.processo p where j.id = :id")
  Optional<JulgamentoColegiado> findByIdWithProcesso(@Param("id") Long id);

  @Query(value = """
      SELECT DISTINCT ON (j.processo_id) j.*
      FROM tb_julgamento_colegiado j
      WHERE j.processo_id = ANY(:processoIds)
        AND j.pauta_data_hora IS NOT NULL
        AND j.pauta_data_hora >= :from
        AND j.status IN ('AGENDADO','EM_ANDAMENTO','SUSPENSO','VISTA','ADIADO')
      ORDER BY j.processo_id, j.pauta_data_hora ASC
      """, nativeQuery = true)
  List<JulgamentoColegiado> findNextPautaByProcessoIds(@Param("processoIds") long[] processoIds,
                                                     @Param("from") LocalDateTime from);

  @Query(value = """
      SELECT j.*
      FROM tb_julgamento_colegiado j
      WHERE j.processo_id = ANY(:processoIds)
        AND j.pauta_data_hora IS NOT NULL
        AND j.pauta_data_hora >= :from
        AND j.pauta_data_hora <= :to
      ORDER BY j.pauta_data_hora ASC
      """, nativeQuery = true)
  List<JulgamentoColegiado> findUpcomingPautaByProcessoIds(@Param("processoIds") long[] processoIds,
                                                          @Param("from") LocalDateTime from,
                                                          @Param("to") LocalDateTime to);


  @Query("""
      select j from JulgamentoColegiado j
      join fetch j.processo p
      where (:colegiado is null or :colegiado = '' or lower(j.orgaoJulgador) like lower(concat('%', :colegiado, '%')))
        and (p.nivelSigilo is null or p.nivelSigilo = com.tcc.pjb.backend.model.entity.enums.NivelSigilo.PUBLICO)
      order by coalesce(j.pautaDataHora, j.sessaoInicio, j.createdAt) desc
      """)
  List<JulgamentoColegiado> findPublicSessoes(@Param("colegiado") String colegiado, Pageable pageable);

  @Query("""
      select j from JulgamentoColegiado j
      join fetch j.processo p
      where j.id = :id
        and (p.nivelSigilo is null or p.nivelSigilo = com.tcc.pjb.backend.model.entity.enums.NivelSigilo.PUBLICO)
      """)
  Optional<JulgamentoColegiado> findPublicByIdWithProcesso(@Param("id") Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select j from JulgamentoColegiado j where j.id = :id")
  Optional<JulgamentoColegiado> findByIdForUpdate(@Param("id") Long id);


  @Query(value = """
      SELECT j.id, j.processo_id, j.pauta_data_hora
      FROM tb_julgamento_colegiado j
      WHERE j.processo_id = ANY(:processoIds)
        AND j.pauta_data_hora IS NOT NULL
        AND j.pauta_data_hora >= :from
        AND j.pauta_data_hora <= :to
      ORDER BY j.pauta_data_hora ASC
      """, nativeQuery = true)
  List<Object[]> findCalendarRowsByProcessoIds(@Param("processoIds") long[] processoIds,
                                               @Param("from") LocalDateTime from,
                                               @Param("to") LocalDateTime to);


  @Query("""
      select j from JulgamentoColegiado j
      join fetch j.processo p
      where (:tribunal is null or :tribunal = '' or upper(coalesce(j.tribunalSigla, '')) = upper(:tribunal))
      order by coalesce(j.pautaDataHora, j.sessaoInicio, j.createdAt) desc, j.id desc
      """)
  List<JulgamentoColegiado> findAllWithProcessoByTribunal(@Param("tribunal") String tribunal);

  @Query("""
      select j from JulgamentoColegiado j
      join fetch j.processo p
      where j.status in :statuses
        and (:tribunal is null or :tribunal = '' or upper(coalesce(j.tribunalSigla, '')) = upper(:tribunal))
      order by coalesce(j.pautaDataHora, j.sessaoInicio, j.createdAt) desc, j.id desc
      """)
  List<JulgamentoColegiado> findByStatusInWithProcessoAndTribunal(@Param("statuses") Collection<StatusJulgamentoColegiado> statuses,
                                                                  @Param("tribunal") String tribunal);

  @Query("""
      select j from JulgamentoColegiado j
      join fetch j.processo p
      where j.status = :status
        and coalesce(j.acordaoPublicado, false) = false
        and (:tribunal is null or :tribunal = '' or upper(coalesce(j.tribunalSigla, '')) = upper(:tribunal))
      order by coalesce(j.sessaoFim, j.updatedAt, j.createdAt) asc, j.id asc
      """)
  List<JulgamentoColegiado> findAguardandoPublicacaoAcordao(@Param("status") StatusJulgamentoColegiado status,
                                                            @Param("tribunal") String tribunal);

}
