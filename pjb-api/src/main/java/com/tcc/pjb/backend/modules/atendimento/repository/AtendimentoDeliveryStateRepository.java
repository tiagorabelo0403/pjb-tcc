package com.tcc.pjb.backend.modules.atendimento.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoDeliveryState;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoDeliveryStateId;

@Repository
public interface AtendimentoDeliveryStateRepository extends JpaRepository<AtendimentoDeliveryState, AtendimentoDeliveryStateId> {

  Optional<AtendimentoDeliveryState> findByThreadIdAndUsuarioId(Long threadId, Long usuarioId);
}
