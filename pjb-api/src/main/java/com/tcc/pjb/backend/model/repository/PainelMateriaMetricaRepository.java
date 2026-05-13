package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.painel.PainelMateriaMetrica;

public interface PainelMateriaMetricaRepository extends JpaRepository<PainelMateriaMetrica, Long> {

    Optional<PainelMateriaMetrica> findByChaveMetrica(String chaveMetrica);

    List<PainelMateriaMetrica> findTop10ByOrderByTotalProcessosDesc();
}
