package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.processo.CargaProcesso;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CargaProcessoRepository extends JpaRepository<CargaProcesso, Long> {

    @Query("SELECT c FROM CargaProcesso c WHERE c.processoId = :processoId AND c.status = 'ATIVA'")
    Optional<CargaProcesso> findCargaAtivaByProcessoId(@Param("processoId") Long processoId);

    List<CargaProcesso> findByProcessoIdOrderByDataSaidaAsc(Long processoId);
}
