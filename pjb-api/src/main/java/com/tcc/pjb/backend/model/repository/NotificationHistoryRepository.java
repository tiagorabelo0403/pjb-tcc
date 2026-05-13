package com.tcc.pjb.backend.model.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.NotificationHistory;

@Repository
public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {

    List<NotificationHistory> findTop50ByUsuarioIdOrderByEnviadoEmDesc(Long usuarioId);

    List<NotificationHistory> findByUsuarioIdOrderByEnviadoEmDesc(Long usuarioId, Pageable pageable);

    List<NotificationHistory> findTop50ByUsuarioIdAndProcessoIdOrderByEnviadoEmDesc(Long usuarioId, Long processoId);

    long countByUsuarioIdAndStatusAndEnviadoEmAfter(Long usuarioId, String status, LocalDateTime after);

    long countByUsuarioIdAndLidoEmIsNull(Long usuarioId);

    boolean existsByUsuarioIdAndProcessoIdAndTituloAndStatusAndEnviadoEmAfter(
            Long usuarioId,
            Long processoId,
            String titulo,
            String status,
            LocalDateTime after
    );

    boolean existsByUsuarioIdAndProcessoIdAndTituloAndCanalAndStatusAndEnviadoEmAfter(
            Long usuarioId,
            Long processoId,
            String titulo,
            String canal,
            String status,
            LocalDateTime after
    );

    Optional<NotificationHistory> findByTrackingToken(String trackingToken);
    boolean existsByUsuarioIdAndProcessoIdAndTitulo(Long usuarioId, Long processoId, String titulo);

}
