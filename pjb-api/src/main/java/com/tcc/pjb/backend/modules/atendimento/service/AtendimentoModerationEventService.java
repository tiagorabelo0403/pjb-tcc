package com.tcc.pjb.backend.modules.atendimento.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoModerationEvent;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThread;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoModerationEventRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtendimentoModerationEventService {

  private final AtendimentoModerationEventRepository repo;
  private final ObjectMapper mapper;

  public AtendimentoModerationEventService(AtendimentoModerationEventRepository repo, ObjectMapper mapper) {
    this.repo = Objects.requireNonNull(repo);
    this.mapper = Objects.requireNonNull(mapper);
  }

  @Transactional
  public void recordBlockedAttempt(Usuario actor, AtendimentoThread thread, String reason, String content) {
    record(actor, thread, reason, content, null);
  }

  @Transactional
  public void recordSystemBlock(Usuario actor, AtendimentoThread thread, String reason, Map<String, Object> meta) {
    record(actor, thread, reason, null, meta);
  }

  @Transactional
  public void recordModeratorAction(Usuario actor, AtendimentoThread thread, String reason, Map<String, Object> meta) {
    record(actor, thread, reason, null, meta);
  }

  private void record(Usuario actor, AtendimentoThread thread, String reason, String content, Map<String, Object> meta) {
    if (actor == null || actor.getId() == null) return;
    String actorTipo = actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : "UNKNOWN";
    String normalizedContent = content != null ? content : "";
    String contentHash = Hashes.sha256Hex(normalizedContent);
    String snippet = buildSnippet(normalizedContent);
    String metadataJson = null;
    if (meta != null && !meta.isEmpty()) {
      try {
        metadataJson = mapper.writeValueAsString(meta);
      } catch (Exception e) {
        throw new IllegalStateException("metadata_serialization_failed", e);
      }
    }

    AtendimentoModerationEvent ev = AtendimentoModerationEvent.builder()
        .createdAt(Instant.now())
        .actorUserId(actor.getId())
        .actorTipo(actorTipo)
        .threadId(thread != null ? thread.getId() : null)
        .processoId(thread != null ? thread.getProcessoId() : null)
        .reason(reason != null ? reason : "unknown")
        .contentHash(contentHash)
        .snippet(snippet)
        .metadataJson(metadataJson)
        .build();
    repo.save(ev);
  }

  private String buildSnippet(String content) {
    if (content == null || content.isBlank()) {
      return null;
    }
    String normalized = content.replaceAll("\\s+", " ").trim();
    return normalized.length() <= 180 ? normalized : normalized.substring(0, 180);
  }
}
