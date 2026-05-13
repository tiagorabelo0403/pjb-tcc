package com.tcc.pjb.backend.modules.atendimento.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoChecklistAuditEvent;

public interface AtendimentoChecklistAuditEventRepository extends JpaRepository<AtendimentoChecklistAuditEvent, Long> {

  Optional<AtendimentoChecklistAuditEvent> findTopByThreadIdOrderByIdDesc(Long threadId);

  Page<AtendimentoChecklistAuditEvent> findByThreadIdOrderByIdDesc(Long threadId, Pageable pageable);

  List<AtendimentoChecklistAuditEvent> findByItemIdOrderByIdAsc(Long itemId);
}
