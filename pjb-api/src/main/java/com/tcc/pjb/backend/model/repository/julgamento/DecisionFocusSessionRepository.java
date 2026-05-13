package com.tcc.pjb.backend.model.repository.julgamento;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.julgamento.DecisionFocusSession;

public interface DecisionFocusSessionRepository extends JpaRepository<DecisionFocusSession, Long> {

    Optional<DecisionFocusSession> findBySessionToken(String sessionToken);

    @Query("""
            select s from DecisionFocusSession s
            where s.usuario.id = :usuarioId
              and s.status in ('OPEN','ARMED')
            order by s.openedAt desc
            """)
    List<DecisionFocusSession> findActiveByUsuario(@Param("usuarioId") Long usuarioId);

    @Query("""
            select s from DecisionFocusSession s
            where s.usuario.id = :usuarioId
              and s.processo.id = :processoId
              and s.status = 'ARMED'
              and s.expiresAt > :now
            order by s.armedAt desc, s.openedAt desc
            """)
    List<DecisionFocusSession> findArmedByUsuarioAndProcesso(@Param("usuarioId") Long usuarioId,
                                                             @Param("processoId") Long processoId,
                                                             @Param("now") Instant now);

    @Query("""
            select s from DecisionFocusSession s
            where s.usuario.id = :usuarioId
            order by coalesce(s.lastCheckedAt, s.armedAt, s.openedAt) desc
            """)
    List<DecisionFocusSession> findRecentByUsuario(@Param("usuarioId") Long usuarioId, Pageable pageable);

    default List<DecisionFocusSession> findTop20RecentByUsuario(Long usuarioId) {
        return findRecentByUsuario(usuarioId, PageRequest.of(0, 20));
    }
}
