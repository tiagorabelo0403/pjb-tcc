package com.tcc.pjb.backend.service.processo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.moderation.TextModerationService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.processo.ProcessoNoteCreateRequest;
import com.tcc.pjb.backend.model.dto.processo.ProcessoNoteDto;
import com.tcc.pjb.backend.model.dto.processo.ProcessoNoteUpdateRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.processo.ProcessoNote;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.processo.ProcessoNoteRepository;

@Service
public class ProcessoNoteService {

  private final CurrentUserService currentUser;
  private final ProcessoRepository processoRepo;
  private final ProcessoNoteRepository noteRepo;
  private final PjbAuthorizationService authz;
  private final TextModerationService moderation;

  public ProcessoNoteService(CurrentUserService currentUser,
                             ProcessoRepository processoRepo,
                             ProcessoNoteRepository noteRepo,
                             PjbAuthorizationService authz,
                             TextModerationService moderation,
                             ObjectMapper mapper) {
    this.currentUser = Objects.requireNonNull(currentUser);
    this.processoRepo = Objects.requireNonNull(processoRepo);
    this.noteRepo = Objects.requireNonNull(noteRepo);
    this.authz = Objects.requireNonNull(authz);
    this.moderation = Objects.requireNonNull(moderation);
    Objects.requireNonNull(mapper);
  }

  @Transactional(readOnly = true)
  public List<ProcessoNoteDto> list(Long processoId) {
    Processo p = processoRepo.findById(processoId).orElseThrow();
    enforceRead(p);
    return noteRepo.findByProcessoIdOrderByUpdatedAtDesc(processoId).stream().map(ProcessoNoteService::toDto).toList();
  }

  @Transactional
  public ProcessoNoteDto create(Long processoId, ProcessoNoteCreateRequest req) {
    String body = normalizeBody(req.body());
    if (body == null) throw new IllegalArgumentException("body");
    body = moderation.validateMessage(body);
    Usuario u = currentUser.getRequired();
    Processo p = processoRepo.findById(processoId).orElseThrow();
    enforceRead(p);
    String tagsJson = normalizeTags(req.tags());
    Instant now = Instant.now();
    ProcessoNote n = noteRepo.save(ProcessoNote.builder()
        .processoId(processoId)
        .authorUsuarioId(u.getId())
        .authorTipo(u.getTipoUsuario() != null ? u.getTipoUsuario().name() : "UNKNOWN")
        .body(body)
        .tagsJson(tagsJson)
        .createdAt(now)
        .updatedAt(now)
        .build());
    return toDto(n);
  }

  @Transactional
  public ProcessoNoteDto update(Long processoId, Long noteId, ProcessoNoteUpdateRequest req) {
    String body = normalizeBody(req.body());
    if (body == null) throw new IllegalArgumentException("body");
    body = moderation.validateMessage(body);
    Usuario u = currentUser.getRequired();
    Processo p = processoRepo.findById(processoId).orElseThrow();
    enforceRead(p);
    ProcessoNote n = noteRepo.findByIdAndProcessoId(noteId, processoId).orElseThrow();
    if (!canEdit(u, n)) throw new AccessDeniedException("Acesso negado");
    n.setBody(body);
    n.setTagsJson(normalizeTags(req.tags()));
    n.setUpdatedAt(Instant.now());
    return toDto(noteRepo.save(n));
  }

  @Transactional
  public void delete(Long processoId, Long noteId) {
    Usuario u = currentUser.getRequired();
    Processo p = processoRepo.findById(processoId).orElseThrow();
    enforceRead(p);
    ProcessoNote n = noteRepo.findByIdAndProcessoId(noteId, processoId).orElseThrow();
    if (!canEdit(u, n)) throw new AccessDeniedException("Acesso negado");
    noteRepo.delete(n);
  }

  private void enforceRead(Processo p) {
    Usuario u = currentUser.getOrNull();
    if (u != null && u.getTipoUsuario() == TipoUsuario.CIDADAO) {
      authz.requireReadProcessoAsCidadaoParte(p);
      return;
    }
    authz.requireReadProcesso(p);
  }

  private static boolean canEdit(Usuario u, ProcessoNote n) {
    if (u == null || u.getId() == null || n == null) return false;
    if (u.getId().equals(n.getAuthorUsuarioId())) return true;
    TipoUsuario t = u.getTipoUsuario();
    return t != null && (t.isServidorJudiciario() || t.isMagistratura() || t.isAdmin());
  }

  private static ProcessoNoteDto toDto(ProcessoNote n) {
    return new ProcessoNoteDto(
        n.getId(),
        n.getProcessoId(),
        n.getAuthorUsuarioId(),
        n.getAuthorTipo(),
        n.getBody(),
        parseTags(n.getTagsJson()),
        n.getCreatedAt(),
        n.getUpdatedAt());
  }

  private static String normalizeBody(String s) {
    if (s == null) return null;
    String t = s.trim();
    if (t.isEmpty()) return null;
    if (t.length() > 4000) t = t.substring(0, 4000);
    return t;
  }

  private static String normalizeTags(List<String> tags) {
    if (tags == null || tags.isEmpty()) {
      return "[]";
    }
    List<String> normalized = new ArrayList<>();
    for (String tag : tags) {
      if (tag == null) {
        continue;
      }
      String cleaned = tag.trim();
      if (cleaned.isEmpty()) {
        continue;
      }
      if (cleaned.length() > 60) {
        cleaned = cleaned.substring(0, 60);
      }
      String candidate = cleaned;
      if (normalized.stream().noneMatch(existing -> existing.equalsIgnoreCase(candidate))) {
        normalized.add(candidate);
      }
    }
    if (normalized.isEmpty()) {
      return "[]";
    }
    return normalized.stream()
        .map(tag -> '"' + escapeJson(tag) + '"')
        .collect(Collectors.joining(",", "[", "]"));
  }

  private static List<String> parseTags(String tagsJson) {
    if (tagsJson == null || tagsJson.isBlank() || "[]".equals(tagsJson.trim())) {
      return List.of();
    }
    String body = tagsJson.trim();
    if (body.startsWith("[") && body.endsWith("]")) {
      body = body.substring(1, body.length() - 1);
    }
    if (body.isBlank()) {
      return List.of();
    }
    List<String> out = new ArrayList<>();
    for (String raw : body.split(",")) {
      String cleaned = raw.trim();
      if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() >= 2) {
        cleaned = cleaned.substring(1, cleaned.length() - 1);
      }
      cleaned = cleaned.replace("\\\"", "\"").replace("\\\\", "\\").trim();
      String candidate = cleaned;
      if (!candidate.isEmpty() && out.stream().noneMatch(existing -> existing.equalsIgnoreCase(candidate))) {
        out.add(candidate);
      }
    }
    return List.copyOf(out);
  }

  private static String escapeJson(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
