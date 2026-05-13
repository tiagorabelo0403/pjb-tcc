package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.audiencia.AudienciaWebRtcSessao;

public interface AudienciaWebRtcSessaoRepository extends JpaRepository<AudienciaWebRtcSessao, Long> {

    Optional<AudienciaWebRtcSessao> findBySessaoToken(String sessaoToken);

    List<AudienciaWebRtcSessao> findTop50ByParticipanteUsuarioIdOrderByCreatedAtDesc(Long participanteUsuarioId);
}
