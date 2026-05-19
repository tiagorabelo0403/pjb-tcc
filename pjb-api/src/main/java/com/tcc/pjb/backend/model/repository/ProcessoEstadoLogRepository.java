package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.processo.ProcessoEstadoLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessoEstadoLogRepository extends JpaRepository<ProcessoEstadoLog, Long> {

    List<ProcessoEstadoLog> findByProcessoIdOrderByCreatedAtAsc(Long processoId);
}
