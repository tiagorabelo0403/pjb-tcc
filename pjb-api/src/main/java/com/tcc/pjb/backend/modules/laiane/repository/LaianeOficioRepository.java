package com.tcc.pjb.backend.modules.laiane.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeOficio;

@Repository
public interface LaianeOficioRepository extends JpaRepository<LaianeOficio, Long> {

    Optional<LaianeOficio> findByTrackingCode(UUID trackingCode);

    @Query("""
            SELECT o FROM LaianeOficio o
            WHERE o.trackingCode = :trackingCode
              AND (o.origem.id = :usuarioId OR o.destino.id = :usuarioId)
            """)
    Optional<LaianeOficio> findByTrackingCodeAndUsuarioEnvolvido(
            @Param("trackingCode") UUID trackingCode,
            @Param("usuarioId") Long usuarioId);

    List<LaianeOficio> findTop50ByOrigem_IdOrderByCreatedAtDesc(Long origemId);

    List<LaianeOficio> findTop50ByDestino_IdOrderByCreatedAtDesc(Long destinoId);

    Optional<LaianeOficio> findFirstByBodyHashAndCreatedAtAfter(String bodyHash, LocalDateTime since);
}
