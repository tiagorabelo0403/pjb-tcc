package com.tcc.pjb.backend.model.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.AcordoHomologado;

@Repository
public interface AcordoHomologadoRepository extends JpaRepository<AcordoHomologado, Long> {

    Optional<AcordoHomologado> findByProcesso_Id(Long processoId);

    Optional<AcordoHomologado> findByProposta_Id(Long propostaId);
}