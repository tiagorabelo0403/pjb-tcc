package com.tcc.pjb.backend.modules.auditoria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditoriaRepository extends JpaRepository<AuditoriaEventoComportamental, Long> {

    @Query("""
        SELECT a FROM AuditoriaEventoComportamental a
        WHERE (:referencia IS NULL OR a.referenciaId = :referencia)
          AND (:acao IS NULL OR a.acao = :acao)
          AND (:usuarioId IS NULL OR a.usuarioId = :usuarioId)
        ORDER BY a.timestamp DESC
    """)
    Page<AuditoriaEventoComportamental> search(
            @Param("referencia") String referencia,
            @Param("acao") String acao,
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );
}
