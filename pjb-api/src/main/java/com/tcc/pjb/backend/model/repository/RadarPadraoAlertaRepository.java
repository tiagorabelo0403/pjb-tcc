package com.tcc.pjb.backend.model.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.radar.RadarPadraoAlerta;

@Repository
public interface RadarPadraoAlertaRepository extends JpaRepository<RadarPadraoAlerta, Long> {

    List<RadarPadraoAlerta> findTop100ByProcessoIdOrderByDetectadoEmDesc(Long processoId);

    List<RadarPadraoAlerta> findTop100ByNupnOrderByDetectadoEmDesc(String nupn);
}
