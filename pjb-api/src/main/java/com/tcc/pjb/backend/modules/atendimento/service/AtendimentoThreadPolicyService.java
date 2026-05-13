package com.tcc.pjb.backend.modules.atendimento.service;

import java.time.Instant;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoThreadPolicyDto;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoUpdateThreadPolicyRequest;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThread;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThreadPolicy;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoThreadPolicyRepository;

@Service
public class AtendimentoThreadPolicyService {

  private final CurrentUserService currentUser;
  private final AtendimentoChatService chat;
  private final AtendimentoThreadPolicyRepository repo;

  public AtendimentoThreadPolicyService(CurrentUserService currentUser,
                                       AtendimentoChatService chat,
                                       AtendimentoThreadPolicyRepository repo) {
    this.currentUser = Objects.requireNonNull(currentUser);
    this.chat = Objects.requireNonNull(chat);
    this.repo = Objects.requireNonNull(repo);
  }

  @Transactional(readOnly = true)
  public AtendimentoThreadPolicyDto get(Long threadId) {
    if (threadId == null) throw new IllegalArgumentException("threadId");
    chat.requireThreadAccess(threadId);
    AtendimentoThreadPolicy p = repo.findById(threadId).orElse(null);
    return p == null ? new AtendimentoThreadPolicyDto(threadId, null, null, null)
        : new AtendimentoThreadPolicyDto(p.getThreadId(), p.getCidadaoSendDisabledUntil(), p.getUpdatedByUserId(), p.getUpdatedAt());
  }

  @Transactional
  public AtendimentoThreadPolicyDto update(Long threadId, AtendimentoUpdateThreadPolicyRequest req) {
    if (threadId == null) throw new IllegalArgumentException("threadId");
    AtendimentoThread t = chat.requireThreadAccess(threadId);
    Usuario u = currentUser.getRequired();
    if (u.getTipoUsuario() != TipoUsuario.ADVOGADO) {
      throw new AccessDeniedException("Somente advogado pode alterar policy do chat");
    }
    
    if (!Objects.equals(t.getAdvogadoId(), u.getId())) {
      throw new AccessDeniedException("Acesso negado");
    }

    Instant now = Instant.now();
    AtendimentoThreadPolicy p = repo.findById(threadId)
        .orElseGet(() -> AtendimentoThreadPolicy.builder()
            .threadId(threadId)
            .createdAt(now)
            .updatedAt(now)
            .build());

    if (req != null) {
      p.setCidadaoSendDisabledUntil(req.cidadaoSendDisabledUntil());
    }

    p.setUpdatedByUserId(u.getId());
    p.setUpdatedAt(now);
    repo.save(p);

    return new AtendimentoThreadPolicyDto(p.getThreadId(), p.getCidadaoSendDisabledUntil(), p.getUpdatedByUserId(), p.getUpdatedAt());
  }

  @Transactional(readOnly = true)
  public boolean isCitizenSendDisabledNow(Long threadId, Instant at) {
    if (threadId == null || at == null) return false;
    AtendimentoThreadPolicy p = repo.findById(threadId).orElse(null);
    Instant until = p != null ? p.getCidadaoSendDisabledUntil() : null;
    return until != null && at.isBefore(until);
  }

  @Transactional(readOnly = true)
  public Instant citizenSendDisabledUntil(Long threadId) {
    AtendimentoThreadPolicy p = threadId == null ? null : repo.findById(threadId).orElse(null);
    return p != null ? p.getCidadaoSendDisabledUntil() : null;
  }
}
