package com.tcc.pjb.backend.modules.atendimento.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoReadState;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoReadStateId;

@Repository
public interface AtendimentoReadStateRepository extends JpaRepository<AtendimentoReadState, AtendimentoReadStateId> {

  Optional<AtendimentoReadState> findByThreadIdAndUsuarioId(Long threadId, Long usuarioId);
}
