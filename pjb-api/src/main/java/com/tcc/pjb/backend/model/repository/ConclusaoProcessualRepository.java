package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.processo.ConclusaoProcessual;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConclusaoProcessualRepository extends JpaRepository<ConclusaoProcessual, Long> {

    List<ConclusaoProcessual> findByMagistradoIdAndStatus(Long magistradoId, String status);

    boolean existsByProcessoIdAndMagistradoIdAndStatus(Long processoId, Long magistradoId, String status);

    @Query("""
            SELECT COUNT(c) > 0 FROM ConclusaoProcessual c
            WHERE c.processoId = :processoId
              AND c.magistradoId = :magistradoId
              AND c.status = 'EXPIRADA'
              AND c.dataConclusao = (SELECT MAX(u.dataConclusao) FROM ConclusaoProcessual u WHERE u.processoId = :processoId)
            """)
    boolean existsUltimaConclusaoExpiradaDoMagistrado(@Param("processoId") Long processoId, @Param("magistradoId") Long magistradoId);

    @Query("""
            SELECT c FROM ConclusaoProcessual c
            WHERE c.status = 'PENDENTE'
              AND c.dataLimite < :agora
            """)
    List<ConclusaoProcessual> findExpiradas(@Param("agora") Instant agora, Pageable pageable);
}
