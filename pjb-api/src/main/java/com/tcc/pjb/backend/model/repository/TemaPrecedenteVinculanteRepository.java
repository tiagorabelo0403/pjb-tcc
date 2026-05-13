package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.judicial.TemaPrecedenteVinculante;

public interface TemaPrecedenteVinculanteRepository extends JpaRepository<TemaPrecedenteVinculante, Long> {

    Optional<TemaPrecedenteVinculante> findByCodigoIgnoreCase(String codigo);

    List<TemaPrecedenteVinculante> findTop50ByStatusOrderByCreatedAtDesc(String status);

    List<TemaPrecedenteVinculante> findTop100ByOrderByCreatedAtDesc();
}
