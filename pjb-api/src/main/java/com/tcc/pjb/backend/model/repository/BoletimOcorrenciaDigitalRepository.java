package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.criminal.BoletimOcorrenciaDigital;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoletimOcorrenciaDigitalRepository extends JpaRepository<BoletimOcorrenciaDigital, Long> {

    Optional<BoletimOcorrenciaDigital> findByUuid(UUID uuid);

    List<BoletimOcorrenciaDigital> findTop100ByUnidadeRegistro_IdInOrderByUpdatedAtDesc(List<Long> unidadeRegistroIds);
}
