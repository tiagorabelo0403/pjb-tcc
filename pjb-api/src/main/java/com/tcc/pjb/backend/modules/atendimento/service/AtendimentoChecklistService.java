package com.tcc.pjb.backend.modules.atendimento.service;

import java.time.Instant;
import java.time.Clock;
import java.time.Duration;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.ui.UiHistoryService;
import com.tcc.pjb.backend.model.dto.ui.UiToken;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoChecklistCreateItemRequest;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoChecklistItemDto;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoChecklistUpdateItemRequest;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoChecklistAuditEvent;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoChecklistItem;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThread;
import com.tcc.pjb.backend.modules.atendimento.model.AtendimentoChecklistAuditEventType;
import com.tcc.pjb.backend.modules.atendimento.model.AtendimentoChecklistItemKind;
import com.tcc.pjb.backend.modules.atendimento.model.AtendimentoChecklistItemStatus;
import com.tcc.pjb.backend.modules.atendimento.model.ChecklistThreadAgg;
import com.tcc.pjb.backend.modules.atendimento.util.ChecklistBadgeUtils;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoChecklistAuditEventRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoChecklistItemRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoThreadRepository;
import com.tcc.pjb.backend.modules.atendimento.service.AtendimentoInboxLiveHub;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoThreadMemberSettingsRepository;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThreadMemberSettings;

@Service
public class AtendimentoChecklistService {

  private static final ZoneId ZONE_NOTIFY = ZoneId.of("America/Fortaleza");

  private final CurrentUserService currentUser;
  private final Clock clock;
  private final AtendimentoChatService chat;
  private final AtendimentoThreadRepository threadRepo;
  private final AtendimentoChecklistItemRepository itemRepo;
  private final AtendimentoChecklistAuditEventRepository auditRepo;
  private final AtendimentoInboxLiveHub liveHub;
  private final UiHistoryService uiHistory;
  private final UsuarioRepository usuarioRepo;
  private final ProcessoRepository processoRepo;
  private final AtendimentoThreadMemberSettingsRepository settingsRepo;
  private final ObjectMapper mapper;

  public AtendimentoChecklistService(CurrentUserService currentUser,
                                    Clock clock,
                                    AtendimentoChatService chat,
                                    AtendimentoThreadRepository threadRepo,
                                    AtendimentoChecklistItemRepository itemRepo,
                                    AtendimentoChecklistAuditEventRepository auditRepo,
                                    AtendimentoInboxLiveHub liveHub,
                                    UiHistoryService uiHistory,
                                    UsuarioRepository usuarioRepo,
                                    ProcessoRepository processoRepo,
                                    AtendimentoThreadMemberSettingsRepository settingsRepo,
                                    ObjectMapper mapper) {
    this.currentUser = Objects.requireNonNull(currentUser);
    this.clock = Objects.requireNonNull(clock);
    this.chat = Objects.requireNonNull(chat);
    this.threadRepo = Objects.requireNonNull(threadRepo);
    this.itemRepo = Objects.requireNonNull(itemRepo);
    this.auditRepo = Objects.requireNonNull(auditRepo);
    this.liveHub = Objects.requireNonNull(liveHub);
    this.uiHistory = Objects.requireNonNull(uiHistory);
    this.usuarioRepo = Objects.requireNonNull(usuarioRepo);
    this.processoRepo = Objects.requireNonNull(processoRepo);
    this.settingsRepo = Objects.requireNonNull(settingsRepo);
    this.mapper = Objects.requireNonNull(mapper);
  }

  @Transactional(readOnly = true)
  public Page<AtendimentoChecklistItemDto> list(Long threadId, int page, int size) {
    if (threadId == null) throw new IllegalArgumentException("threadId");
    chat.requireThreadAccess(threadId);

    Pageable p = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    Page<AtendimentoChecklistItem> items = itemRepo.findByThreadIdOrderByIdDesc(threadId, p);

    return items.map(AtendimentoChecklistService::toDto);
  }

  @Transactional
  public AtendimentoChecklistItemDto create(Long threadId, AtendimentoChecklistCreateItemRequest req) {
    if (threadId == null) throw new IllegalArgumentException("threadId");
    if (req == null) throw new IllegalArgumentException("req");

    Usuario actor = currentUser.getRequired();
    requireAdvogado(actor);

    
    chat.requireThreadAccess(threadId);
    AtendimentoThread t = threadRepo.findByIdForUpdate(threadId).orElseThrow();

    Instant now = Instant.now(clock);
    AtendimentoChecklistItemKind kind = parseKind(req.kind());
    String title = normalizeTitle(req.title());
    String note = normalizeNote(req.note());

    AtendimentoChecklistItem item = itemRepo.save(AtendimentoChecklistItem.builder()
        .threadId(threadId)
        .kind(kind)
        .status(AtendimentoChecklistItemStatus.OPEN)
        .title(title)
        .note(note)
        .dueAt(req.dueAt())
        .documentoId(req.documentoId())
        .createdByUserId(actor.getId())
        .createdAt(now)
        .updatedAt(now)
        .build());

    Map<String, Object> auditPayload = new LinkedHashMap<>();
    auditPayload.put("kind", kind.name());
    auditPayload.put("title", title);
    if (req.dueAt() != null) {
      auditPayload.put("dueAt", req.dueAt().toString());
    }
    if (req.documentoId() != null) {
      auditPayload.put("documentoId", req.documentoId());
    }
    if (note != null && !note.isBlank()) {
      auditPayload.put("note", note);
    }
    recordAudit(t.getId(), item.getId(), actor.getId(), AtendimentoChecklistAuditEventType.ITEM_CREATED, auditPayload, now);

    publishChecklistUpdated(t, actor, item.getId(), "ITEM_CREATED", now);

    return toDto(item);
  }

  @Transactional
  public AtendimentoChecklistItemDto update(Long threadId, Long itemId, AtendimentoChecklistUpdateItemRequest req) {
    if (threadId == null) throw new IllegalArgumentException("threadId");
    if (itemId == null) throw new IllegalArgumentException("itemId");

    Usuario actor = currentUser.getRequired();
    requireAdvogado(actor);

    chat.requireThreadAccess(threadId);
    AtendimentoThread t = threadRepo.findByIdForUpdate(threadId).orElseThrow();

    AtendimentoChecklistItem item = itemRepo.findById(itemId).orElseThrow();
    if (!Objects.equals(item.getThreadId(), threadId)) throw new AccessDeniedException("Acesso negado");
    if (item.getStatus() == AtendimentoChecklistItemStatus.DONE || item.getStatus() == AtendimentoChecklistItemStatus.CANCELLED) {
      throw new IllegalStateException("item_readonly");
    }

    Instant now = Instant.now(clock);

    String newTitle = req != null && req.title() != null ? normalizeTitle(req.title()) : null;
    String newNote = req != null ? normalizeNote(req.note()) : null;

    boolean changed = false;
    if (newTitle != null && !newTitle.equals(item.getTitle())) {
      item.setTitle(newTitle);
      changed = true;
    }
    if (req != null && req.dueAt() != null) {
      item.setDueAt(req.dueAt());
      changed = true;
    }
    if (req != null && req.documentoId() != null) {
      item.setDocumentoId(req.documentoId());
      changed = true;
    }
    if (newNote != null && !Objects.equals(newNote, item.getNote())) {
      item.setNote(newNote);
      changed = true;
    }

    if (changed) {
      item.setUpdatedAt(now);
      itemRepo.save(item);

      recordAudit(t.getId(), item.getId(), actor.getId(), AtendimentoChecklistAuditEventType.ITEM_UPDATED,
          PayloadMaps.ofEntries(
              "title", item.getTitle(),
              "dueAt", item.getDueAt() != null ? item.getDueAt().toString() : null,
              "documentoId", item.getDocumentoId(),
              "note", item.getNote()
          ), now);

      publishChecklistUpdated(t, actor, item.getId(), "ITEM_UPDATED", now);
    }

    return toDto(item);
  }

  @Transactional
  public AtendimentoChecklistItemDto markDone(Long threadId, Long itemId) {
    return transition(threadId, itemId, AtendimentoChecklistItemStatus.DONE, AtendimentoChecklistAuditEventType.ITEM_DONE);
  }

  @Transactional
  public AtendimentoChecklistItemDto reopen(Long threadId, Long itemId) {
    return transition(threadId, itemId, AtendimentoChecklistItemStatus.OPEN, AtendimentoChecklistAuditEventType.ITEM_REOPENED);
  }

  @Transactional
public AtendimentoChecklistItemDto cancel(Long threadId, Long itemId) {
  return transition(threadId, itemId, AtendimentoChecklistItemStatus.CANCELLED, AtendimentoChecklistAuditEventType.ITEM_CANCELLED);
}


  private AtendimentoChecklistItemDto transition(Long threadId, Long itemId, AtendimentoChecklistItemStatus target, AtendimentoChecklistAuditEventType evt) {
  if (threadId == null) throw new IllegalArgumentException("threadId");
  if (itemId == null) throw new IllegalArgumentException("itemId");
  if (target == null) throw new IllegalArgumentException("target");

  Usuario actor = currentUser.getRequired();
  requireAdvogado(actor);

  chat.requireThreadAccess(threadId);
  AtendimentoThread t = threadRepo.findByIdForUpdate(threadId).orElseThrow();

  AtendimentoChecklistItem item = itemRepo.findById(itemId).orElseThrow();
  if (!Objects.equals(item.getThreadId(), threadId)) throw new AccessDeniedException("Acesso negado");

  AtendimentoChecklistItemStatus cur = item.getStatus();
  if (cur == null) cur = AtendimentoChecklistItemStatus.OPEN;

  
  if (cur == target) {
    return toDto(item);
  }

  
  
  
  if (cur == AtendimentoChecklistItemStatus.CANCELLED && target == AtendimentoChecklistItemStatus.DONE) {
    throw new IllegalStateException("item_cancelled");
  }

  Instant now = Instant.now(clock);
  item.setStatus(target);

  if (target == AtendimentoChecklistItemStatus.DONE) {
    
    item.setCancelledAt(null);
    item.setCancelledByUserId(null);
    item.setCompletedAt(now);
    item.setCompletedByUserId(actor.getId());
  } else if (target == AtendimentoChecklistItemStatus.CANCELLED) {
    
    item.setCompletedAt(null);
    item.setCompletedByUserId(null);
    item.setCancelledAt(now);
    item.setCancelledByUserId(actor.getId());
  } else if (target == AtendimentoChecklistItemStatus.OPEN) {
    
    item.setCompletedAt(null);
    item.setCompletedByUserId(null);
    item.setCancelledAt(null);
    item.setCancelledByUserId(null);
  }

  item.setUpdatedAt(now);
  itemRepo.save(item);

  recordAudit(t.getId(), item.getId(), actor.getId(), evt, PayloadMaps.ofEntries("status", item.getStatus() != null ? item.getStatus().name() : null), now);
  publishChecklistUpdated(t, actor, item.getId(), evt != null ? evt.name() : "STATUS_CHANGED", now);

  return toDto(item);
}

private void recordAudit(Long threadId, Long itemId, Long actorId, AtendimentoChecklistAuditEventType type, Map<String, Object> payload, Instant now) {
    String payloadJson;
    try {
      payloadJson = mapper.writeValueAsString(payload);
    } catch (Exception e) {
      payloadJson = String.valueOf(payload);
    }
    String payloadHash = Hashes.sha256Hex(payloadJson);

    String prev = auditRepo.findTopByThreadIdOrderByIdDesc(threadId)
        .map(AtendimentoChecklistAuditEvent::getChainHash)
        .orElse(null);

    String chainHash = Hashes.sha256Hex((prev != null ? prev : "")
        + "|" + threadId
        + "|" + itemId
        + "|" + actorId
        + "|" + (type != null ? type.name() : "")
        + "|" + payloadHash
        + "|" + (now != null ? now.toEpochMilli() : 0));

    auditRepo.save(AtendimentoChecklistAuditEvent.builder()
        .threadId(threadId)
        .itemId(itemId)
        .actorUserId(actorId)
        .eventType(type)
        .payloadJson(payloadJson)
        .payloadHash(payloadHash)
        .prevHash(prev)
        .chainHash(chainHash)
        .createdAt(now != null ? now : Instant.now(clock))
        .build());
  }



  private void publishChecklistUpdated(AtendimentoThread t, Usuario actor, Long itemId, String action, Instant now) {
    if (t == null || actor == null) return;
    Instant at = now != null ? now : Instant.now();

    ChecklistThreadAgg agg = safeChecklistAgg(t.getId(), at);
    int openCount = agg.openCount();
    int overdueCount = agg.overdueCount();
    Instant nextDueAt = agg.nextDueAt();


    try {
      String json = mapper.writeValueAsString(PayloadMaps.ofEntries(
          "type", "ATENDIMENTO_CHECKLIST_UPDATED",
          "threadId", t.getId(),
          "processoId", t.getProcessoId(),
          "itemId", itemId,
          "action", action,
          "openChecklistCount", openCount,
          "overdueChecklistCount", overdueCount,
          "nextChecklistDueAt", nextDueAt != null ? nextDueAt.toString() : null,
          "nextChecklistDueInMinutes", ChecklistBadgeUtils.computeNextDueInMinutes(at, nextDueAt),
          "overdueSinceMinutes", ChecklistBadgeUtils.computeOverdueSinceMinutes(at, agg),
          "at", at.toString()
      ));
      if (t.getAdvogadoId() != null) liveHub.enqueue(topicForUser(t.getAdvogadoId()), json);
      if (t.getCidadaoUsuarioId() != null) liveHub.enqueue(topicForUser(t.getCidadaoUsuarioId()), json);
    } catch (Exception ignored) {
    }

    
    Long otherId = Objects.equals(actor.getId(), t.getAdvogadoId()) ? t.getCidadaoUsuarioId() : t.getAdvogadoId();
    if (otherId == null) return;

    Usuario other = usuarioRepo.findById(otherId).orElse(null);
    String inboxKey = inboxKeyForUser(other, otherId);

    Processo pr = processoRepo.findById(t.getProcessoId()).orElse(null);
    String numero = pr != null ? pr.getNumeroUnificado() : null;

    EnumSet<UiToken> tok = resolveChecklistInboxTokens(t.getId(), otherId, openCount, overdueCount, at);
    Long nextMin = ChecklistBadgeUtils.computeNextDueInMinutes(at, nextDueAt);
    Long overdueMin = ChecklistBadgeUtils.computeOverdueSinceMinutes(at, agg);
    String msg = "NOTIFICADO: checklist atualizado" + (numero == null ? "" : " • Processo " + numero)
        + " • Pendências: " + openCount + (overdueCount > 0 ? " • Atrasadas: " + overdueCount : "")
        + (nextDueAt != null && nextMin != null ? " • Próx.: em " + nextMin + " min • " + nextDueAt.toString() : "")
        + (overdueCount > 0 && overdueMin != null ? " • Atrasado há " + overdueMin + " min" : "");

    try {
      uiHistory.recordInboxEvent(
          inboxKey,
          t.getProcessoId(),
          UiHistoryService.EVT_ATENDIMENTO_CHECKLIST_UPDATED,
          tok,
          actor.getId(),
          actor.getTipoUsuario() == null ? null : actor.getTipoUsuario().name(),
          msg
      );
    } catch (Exception ignored) {
    }
  }

  private EnumSet<UiToken> resolveChecklistInboxTokens(Long threadId, Long userId, int openCount, int overdueCount, Instant at) {
    AtendimentoThreadMemberSettings st = null;
    try {
      st = settingsRepo.findByThreadIdAndUsuarioId(threadId, userId).orElse(null);
    } catch (Exception ignored) {
    }

    boolean muted = st != null && isMutedNow(st, at != null ? at : Instant.now(clock));
    if (muted) {
      return EnumSet.of(UiToken.INFO);
    }

    if (overdueCount > 0) {
      return EnumSet.of(UiToken.NOTIFICADO, UiToken.URGENTE, UiToken.ATRASADO, UiToken.PENDENTE);
    }
    if (openCount > 0) {
      return EnumSet.of(UiToken.NOTIFICADO, UiToken.PENDENTE);
    }
    return EnumSet.of(UiToken.NOTIFICADO, UiToken.CONCLUIDO);
  }

  private static String inboxKeyForUser(Usuario u, Long userId) {
    if (u != null && u.getTipoUsuario() == TipoUsuario.CIDADAO) {
      String cpf = u.getCpf();
      if (cpf != null && !cpf.isBlank()) {
        return "CIDCPF:" + cpf.trim();
      }
    }
    return "USR:" + userId;
  }

  private static String topicForUser(Long usuarioId) {
    return "ATEND:USR:" + usuarioId;
  }

  private static boolean isMutedNow(AtendimentoThreadMemberSettings s, Instant at) {
    if (s == null || at == null) return false;
    Instant mu = s.getMutedUntil();
    if (mu != null && at.isBefore(mu)) return true;
    return isQuietHoursActive(s, at);
  }

  private static boolean isQuietHoursActive(AtendimentoThreadMemberSettings s, Instant at) {
    if (s == null || at == null) return false;
    Short start = s.getQuietHoursStartMin();
    Short end = s.getQuietHoursEndMin();
    Integer mask = s.getQuietDaysMask();
    if (start == null || end == null || mask == null) return false;

    ZonedDateTime z = at.atZone(ZONE_NOTIFY);
    int dowBit = dayMaskBit(z.getDayOfWeek());
    if ((mask & dowBit) == 0) return false;

    int minutes = z.getHour() * 60 + z.getMinute();
    int sMin = start;
    int eMin = end;

    if (sMin == eMin) {
      return true;
    }
    if (sMin < eMin) {
      return minutes >= sMin && minutes < eMin;
    }
    return minutes >= sMin || minutes < eMin;
  }

  private static int dayMaskBit(DayOfWeek d) {
    return switch (d) {
      case SUNDAY -> 1;
      case MONDAY -> 2;
      case TUESDAY -> 4;
      case WEDNESDAY -> 8;
      case THURSDAY -> 16;
      case FRIDAY -> 32;
      case SATURDAY -> 64;
    };
  }
  private static AtendimentoChecklistItemDto toDto(AtendimentoChecklistItem x) {
    if (x == null) return null;
    return new AtendimentoChecklistItemDto(
        x.getId(),
        x.getThreadId(),
        x.getKind() != null ? x.getKind().name() : null,
        x.getStatus() != null ? x.getStatus().name() : null,
        x.getTitle(),
        x.getNote(),
        x.getDueAt(),
        x.getDocumentoId(),
        x.getCreatedByUserId(),
        x.getCreatedAt(),
        x.getUpdatedAt(),
        x.getCompletedAt(),
        x.getCompletedByUserId(),
        x.getCancelledAt(),
        x.getCancelledByUserId()
    );
  }

  private static void requireAdvogado(Usuario u) {
    if (u == null || u.getTipoUsuario() != TipoUsuario.ADVOGADO) {
      throw new AccessDeniedException("Acesso negado");
    }
  }

  private static AtendimentoChecklistItemKind parseKind(String raw) {
    if (raw == null || raw.isBlank()) return AtendimentoChecklistItemKind.OUTRO;
    try {
      return AtendimentoChecklistItemKind.valueOf(raw.trim().toUpperCase());
    } catch (Exception e) {
      return AtendimentoChecklistItemKind.OUTRO;
    }
  }

  private static String normalizeTitle(String t) {
    if (t == null) throw new IllegalArgumentException("title");
    String x = t.trim();
    if (x.isBlank()) throw new IllegalArgumentException("title");
    if (x.length() > 200) x = x.substring(0, 200);
    return x;
  }

  private static String normalizeNote(String n) {
    if (n == null) return null;
    String x = n.trim();
    if (x.isBlank()) return null;
    if (x.length() > 800) x = x.substring(0, 800);
    return x;
  }

  private ChecklistThreadAgg safeChecklistAgg(Long threadId, Instant now) {
    if (threadId == null) return ChecklistThreadAgg.empty();
    Instant ref = now != null ? now : Instant.now(clock);
    try {
      List<AtendimentoChecklistItemRepository.ThreadChecklistAgg> rows =
          itemRepo.aggregateByThreadIds(List.of(threadId), AtendimentoChecklistItemStatus.OPEN, ref);
      if (rows == null || rows.isEmpty()) return ChecklistThreadAgg.empty();
      AtendimentoChecklistItemRepository.ThreadChecklistAgg r = rows.get(0);
      if (r == null) return ChecklistThreadAgg.empty();
      return new ChecklistThreadAgg(clampToInt(r.getOpenCnt()), clampToInt(r.getOverdueCnt()), r.getNextDueAt(), r.getOldestOverdueAt());
    } catch (Exception ignored) {
      return ChecklistThreadAgg.empty();
    }
  }

  private static int clampToInt(long v) {
    if (v <= 0) return 0;
    if (v >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
    return (int) v;
  }

}
