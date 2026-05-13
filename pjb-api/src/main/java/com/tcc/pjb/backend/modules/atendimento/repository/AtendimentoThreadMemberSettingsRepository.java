package com.tcc.pjb.backend.modules.atendimento.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThreadMemberSettings;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThreadMemberSettingsId;

public interface AtendimentoThreadMemberSettingsRepository extends JpaRepository<AtendimentoThreadMemberSettings, AtendimentoThreadMemberSettingsId> {
  Optional<AtendimentoThreadMemberSettings> findByThreadIdAndUsuarioId(Long threadId, Long usuarioId);

  
  List<AtendimentoThreadMemberSettings> findByUsuarioIdAndThreadIdIn(Long usuarioId, List<Long> threadIds);
}
