package com.tcc.pjb.backend.modules.atendimento.service;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoModerationEventDto;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoModerationEvent;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoModerationEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtendimentoModerationEventQueryService {

  private final CurrentUserService currentUser;
  private final AtendimentoModerationEventRepository repo;

  public AtendimentoModerationEventQueryService(CurrentUserService currentUser, AtendimentoModerationEventRepository repo) {
    this.currentUser = Objects.requireNonNull(currentUser);
    this.repo = Objects.requireNonNull(repo);
  }

  @Transactional(readOnly = true)
  public Page<AtendimentoModerationEventDto> list(Instant from, Instant to, int page, int size) {
    Usuario u = currentUser.getRequired();
    if (!isInstitutional(u)) throw new AccessDeniedException("Acesso negado");

    Instant f = from != null ? from : Instant.now().minusSeconds(7 * 86400L);
    Instant t = to != null ? to : Instant.now();
    Pageable p = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));

    Page<AtendimentoModerationEvent> out = repo.findByCreatedAtBetweenOrderByIdDesc(f, t, p);
    if (out.isEmpty()) return new PageImpl<>(List.of(), p, 0);

    List<AtendimentoModerationEventDto> rows = out.getContent().stream()
        .map(e -> new AtendimentoModerationEventDto(e.getId(), e.getCreatedAt(), e.getActorUserId(), e.getActorTipo(), e.getThreadId(), e.getProcessoId(), e.getReason()))
        .toList();

    return new PageImpl<>(rows, p, out.getTotalElements());
  }

  private static boolean isInstitutional(Usuario u) {
    TipoUsuario t = u != null ? u.getTipoUsuario() : null;
    if (t == null) return false;
    return t.isAdmin() || t.isMagistratura() || t.isServidorJudiciario();
  }
}
