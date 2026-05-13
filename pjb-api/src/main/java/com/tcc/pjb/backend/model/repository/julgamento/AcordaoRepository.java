package com.tcc.pjb.backend.model.repository.julgamento;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.julgamento.Acordao;

public interface AcordaoRepository extends JpaRepository<Acordao, Long> {

  @Query("select a from Acordao a where a.julgamento.id = :jid")
  Optional<Acordao> findByJulgamentoId(@Param("jid") Long julgamentoId);

  @Query("select a from Acordao a where a.julgamento.id in :jids")
  java.util.List<Acordao> findByJulgamentoIdIn(@Param("jids") java.util.List<Long> julgamentoIds);

  @Query("""
      select count(a) from Acordao a
      join a.julgamento j
      where (:tribunal is null or :tribunal = '' or upper(coalesce(j.tribunalSigla, '')) = upper(:tribunal))
      """)
  long countByTribunalSigla(@Param("tribunal") String tribunal);
}
