package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.criminal.BoletimOcorrenciaInqueritoVinculo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoletimOcorrenciaInqueritoVinculoRepository extends JpaRepository<BoletimOcorrenciaInqueritoVinculo, Long> {

    Optional<BoletimOcorrenciaInqueritoVinculo> findByBoletim_Id(Long boletimId);

    List<BoletimOcorrenciaInqueritoVinculo> findTop100ByInquerito_IdOrderByVinculadoEmDesc(Long inqueritoId);
}
