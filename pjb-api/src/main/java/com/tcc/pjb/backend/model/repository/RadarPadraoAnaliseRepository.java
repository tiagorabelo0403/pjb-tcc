package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.radar.RadarPadraoAnalise;

@Repository
public interface RadarPadraoAnaliseRepository extends JpaRepository<RadarPadraoAnalise, Long> {

    Optional<RadarPadraoAnalise> findTopByProcessoIdOrderByGeradoEmDesc(Long processoId);

    List<RadarPadraoAnalise> findTop200ByEscritorioOabHashOrderByGeradoEmDesc(String escritorioOabHash);

    List<RadarPadraoAnalise> findTop200ByDocumentoAutorHashOrderByGeradoEmDesc(String documentoAutorHash);

    List<RadarPadraoAnalise> findTop200ByDocumentoReuHashOrderByGeradoEmDesc(String documentoReuHash);

    List<RadarPadraoAnalise> findTop200ByFingerprintEstruturaHashOrderByGeradoEmDesc(String fingerprintEstruturaHash);

    List<RadarPadraoAnalise> findTop200ByFingerprintConteudoHashOrderByGeradoEmDesc(String fingerprintConteudoHash);

    List<RadarPadraoAnalise> findTop50ByNupnOrderByGeradoEmDesc(String nupn);
}
