package com.tcc.pjb.backend.modules.laiane.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeOficio;

@Repository
public interface LaianeOficioRepository extends JpaRepository<LaianeOficio, Long> {

    Optional<LaianeOficio> findByTrackingCode(UUID trackingCode);

    List<LaianeOficio> findTop50ByOrigem_IdOrderByCreatedAtDesc(Long origemId);

    List<LaianeOficio> findTop50ByDestino_IdOrderByCreatedAtDesc(Long destinoId);
}
