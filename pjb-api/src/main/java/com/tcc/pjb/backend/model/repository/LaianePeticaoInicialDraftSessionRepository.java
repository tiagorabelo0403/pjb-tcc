package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.intelligence.LaianePeticaoInicialDraftSession;

public interface LaianePeticaoInicialDraftSessionRepository extends JpaRepository<LaianePeticaoInicialDraftSession, Long> {

    List<LaianePeticaoInicialDraftSession> findTop100BySolicitante_IdOrderByCreatedAtDesc(Long solicitanteId);

    Optional<LaianePeticaoInicialDraftSession> findByIdAndSolicitante_Id(Long id, Long solicitanteId);

    Optional<LaianePeticaoInicialDraftSession> findByProcesso_Id(Long processoId);
}
