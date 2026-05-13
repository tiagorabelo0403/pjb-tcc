package com.tcc.pjb.backend.model.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.EventoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusEventoInstitucional;

@Repository
public interface EventoInstitucionalRepository extends JpaRepository<EventoInstitucional, Long>, JpaSpecificationExecutor<EventoInstitucional> {

    List<EventoInstitucional> findTop100ByUfOrderBySeveridadeDescCriadoEmDesc(String uf);

    List<EventoInstitucional> findTop100ByUfAndStatusInOrderBySeveridadeDescCriadoEmDesc(String uf, List<StatusEventoInstitucional> status);

    List<EventoInstitucional> findTop100ByCriadoPorUsuarioIdOrderByCriadoEmDesc(Long criadoPorUsuarioId);
}
