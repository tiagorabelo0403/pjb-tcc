package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.juiz.JudicialVoiceSession;

public interface JudicialVoiceSessionRepository extends JpaRepository<JudicialVoiceSession, Long> {

    Optional<JudicialVoiceSession> findByIdAndMagistrado_Id(Long id, Long magistradoId);

    List<JudicialVoiceSession> findTop50ByMagistrado_IdOrderByCreatedAtDesc(Long magistradoId);
}
