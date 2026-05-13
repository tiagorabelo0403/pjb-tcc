package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.IntimacaoAudiencia;
import com.tcc.pjb.backend.model.entity.enums.StatusIntimacaoAudiencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IntimacaoAudienciaRepository extends JpaRepository<IntimacaoAudiencia, Long> {

    List<IntimacaoAudiencia> findByAudiencia_IdOrderByCriadoEmAsc(Long audienciaId);

    List<IntimacaoAudiencia> findByAudiencia_IdAndStatus(Long audienciaId, StatusIntimacaoAudiencia status);

    long countByAudiencia_IdAndStatus(Long audienciaId, StatusIntimacaoAudiencia status);
}
