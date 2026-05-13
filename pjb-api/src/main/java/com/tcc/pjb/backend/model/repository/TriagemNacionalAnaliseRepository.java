package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.triagem.TriagemNacionalAnalise;

@Repository
public interface TriagemNacionalAnaliseRepository extends JpaRepository<TriagemNacionalAnalise, UUID> {

    Optional<TriagemNacionalAnalise> findTopByNupnProvisorioOrderByTriadoEmDesc(String nupnProvisorio);

    Optional<TriagemNacionalAnalise> findTopByProcessoIdOrderByTriadoEmDesc(Long processoId);

    List<TriagemNacionalAnalise> findAllByDocumentoAutorOrDocumentoReuOrderByTriadoEmDesc(String documentoAutor, String documentoReu);
}
