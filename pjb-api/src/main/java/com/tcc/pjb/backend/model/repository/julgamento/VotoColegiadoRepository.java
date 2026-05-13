package com.tcc.pjb.backend.model.repository.julgamento;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.julgamento.VotoColegiado;

public interface VotoColegiadoRepository extends JpaRepository<VotoColegiado, Long> {

  @Query("select v from VotoColegiado v where v.julgamento.id = :jid order by v.ordem asc")
  List<VotoColegiado> findByJulgamentoIdOrdered(@Param("jid") Long julgamentoId);

  @Query("select v from VotoColegiado v where v.julgamento.id = :jid and v.ordem = :ord")
  Optional<VotoColegiado> findByJulgamentoIdAndOrdem(@Param("jid") Long julgamentoId, @Param("ord") int ordem);
}
