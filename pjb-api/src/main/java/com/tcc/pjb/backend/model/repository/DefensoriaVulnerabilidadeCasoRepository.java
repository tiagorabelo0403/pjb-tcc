package com.tcc.pjb.backend.model.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.defensor.DefensoriaVulnerabilidadeCaso;

public interface DefensoriaVulnerabilidadeCasoRepository extends JpaRepository<DefensoriaVulnerabilidadeCaso, Long> {

    List<DefensoriaVulnerabilidadeCaso> findTop100ByDefensor_IdOrderByCreatedAtDesc(Long defensorId);

    List<DefensoriaVulnerabilidadeCaso> findTop100ByDefensor_IdAndStatusOrderByCreatedAtDesc(Long defensorId, String status);
}
