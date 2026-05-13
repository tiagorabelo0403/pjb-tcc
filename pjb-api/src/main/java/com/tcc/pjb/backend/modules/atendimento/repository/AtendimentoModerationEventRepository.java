package com.tcc.pjb.backend.modules.atendimento.repository;

import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoModerationEvent;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtendimentoModerationEventRepository extends JpaRepository<AtendimentoModerationEvent, Long> {

  Page<AtendimentoModerationEvent> findByCreatedAtBetweenOrderByIdDesc(Instant from, Instant to, Pageable pageable);

}
