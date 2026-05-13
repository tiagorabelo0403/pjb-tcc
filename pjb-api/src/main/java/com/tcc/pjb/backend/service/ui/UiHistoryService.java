package com.tcc.pjb.backend.service.ui;

import com.tcc.pjb.backend.core.util.Hashes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.dto.ui.UiHistoryEntryDto;
import com.tcc.pjb.backend.model.dto.ui.UiToken;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.ui.UiStateHistory;
import com.tcc.pjb.backend.model.entity.ui.UiSubjectType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.repository.ui.UiStateHistoryRepository;
import com.tcc.pjb.backend.service.cidadao.dashboard.CidadaoDashboardRefreshRequest;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;

@Service
public class UiHistoryService {

  public static final String EVT_WORKITEM_STATUS_CHANGED = "WORKITEM_STATUS_CHANGED";
  public static final String EVT_PROCESSO_STATUS_CHANGED = "PROCESSO_STATUS_CHANGED";
  public static final String EVT_ATENDIMENTO_NEW_MESSAGE = "ATENDIMENTO_NEW_MESSAGE";
  public static final String EVT_ATENDIMENTO_REMINDER = "ATENDIMENTO_REMINDER";
  public static final String EVT_ATENDIMENTO_CHECKLIST_UPDATED = "ATENDIMENTO_CHECKLIST_UPDATED";

  private final UiStateHistoryRepository repo;
  private final ObjectMapper mapper;
  private final UiHintService ui;
  private final CurrentUserService currentUser;
  private final OutboxPublisher outbox;

  public UiHistoryService(
      UiStateHistoryRepository repo,
      ObjectMapper mapper,
      UiHintService ui,
      CurrentUserService currentUser,
      OutboxPublisher outbox
  ) {
    this.repo = Objects.requireNonNull(repo, "repo");
    this.mapper = Objects.requireNonNull(mapper, "mapper");
    this.ui = Objects.requireNonNull(ui, "ui");
    this.currentUser = Objects.requireNonNull(currentUser, "currentUser");
    this.outbox = Objects.requireNonNull(outbox, "outbox");
  }

  @Transactional
  public void recordWorkItemChange(WorkItem before, WorkItem after, String message) {
    Objects.requireNonNull(before, "before");
    Objects.requireNonNull(after, "after");

    Usuario actor = currentUser.getOrNull();
    Long actorId = actor != null ? actor.getId() : null;
    String actorRole = actor != null && actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : null;

    EnumSet<UiToken> from = ui.tokenSetForWorkItem(before);
    EnumSet<UiToken> to = ui.tokenSetForWorkItem(after);

    UiStateHistory e = new UiStateHistory(
        UUID.randomUUID(),
        UiSubjectType.WORKITEM,
        safeProcessoId(after),
        after.getId(),
        normalizeNullable(after.getInboxKey()),
        EVT_WORKITEM_STATUS_CHANGED,
        before.getStatus() == null ? null : before.getStatus().name(),
        after.getStatus() == null ? null : after.getStatus().name(),
        tokensJson(from),
        tokensJson(to),
        actorId,
        actorRole,
        message,
        Instant.now()
    );

    repo.save(e);
    publishLive(e, from, to);
  }

  @Transactional
  public void recordProcessoStatusChange(Processo processoAtual, StatusProcesso from, String fromResultado, StatusProcesso to,
      String toResultado, String message) {
    Objects.requireNonNull(processoAtual, "processoAtual");

    Usuario actor = currentUser.getOrNull();
    Long actorId = actor != null ? actor.getId() : null;
    String actorRole = actor != null && actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : null;

    Processo snapFrom = snapshot(processoAtual, from, fromResultado);
    Processo snapTo = snapshot(processoAtual, to, toResultado);

    EnumSet<UiToken> fromTok = ui.tokenSetForProcess(snapFrom);
    EnumSet<UiToken> toTok = ui.tokenSetForProcess(snapTo);

    UiStateHistory e = new UiStateHistory(
        UUID.randomUUID(),
        UiSubjectType.PROCESSO,
        processoAtual.getId(),
        null,
        null,
        EVT_PROCESSO_STATUS_CHANGED,
        from == null ? null : from.name(),
        to == null ? null : to.name(),
        tokensJson(fromTok),
        tokensJson(toTok),
        actorId,
        actorRole,
        message,
        Instant.now()
    );

    repo.save(e);
    publishLive(e, fromTok, toTok);
    publishCidadaoPanel(processoAtual, e, fromTok, toTok);
  }

  
  @Transactional
  public void recordInboxEvent(
      String inboxKey,
      Long processoId,
      String eventType,
      EnumSet<UiToken> tokens,
      Long actorUserId,
      String actorRole,
      String message
  ) {
    if (inboxKey == null || inboxKey.isBlank()) {
      throw new IllegalArgumentException("inboxKey");
    }
    if (eventType == null || eventType.isBlank()) {
      throw new IllegalArgumentException("eventType");
    }

    EnumSet<UiToken> tok = tokens == null || tokens.isEmpty() ? EnumSet.of(UiToken.INFO) : tokens;

    UiStateHistory e = new UiStateHistory(
        UUID.randomUUID(),
        UiSubjectType.INBOX,
        processoId,
        null,
        inboxKey,
        eventType,
        null,
        null,
        tokensJson(tok),
        tokensJson(tok),
        actorUserId,
        actorRole,
        message,
        Instant.now()
    );

    repo.save(e);
    publishLive(e, tok, tok);
  }

  @Transactional(readOnly = true)
  public Page<UiHistoryEntryDto> historyByProcessoId(Long processoId, Pageable pageable) {
    Page<UiStateHistory> p = repo.findByProcessoIdOrderByOccurredAtDesc(processoId, pageable);
    return p.map(this::toDto);
  }

  public String etagForProcessoHistory(Long processoId, Pageable pageable) {
    Objects.requireNonNull(processoId, "processoId");
    Objects.requireNonNull(pageable, "pageable");
    Object[] sig = repo.signatureProcesso(processoId);
    return buildEtag("p:" + processoId, sig, pageable);
  }

  @Transactional(readOnly = true)
  public Page<UiHistoryEntryDto> historyByWorkItemId(Long workItemId, Pageable pageable) {
    Page<UiStateHistory> p = repo.findByWorkItemIdOrderByOccurredAtDesc(workItemId, pageable);
    return p.map(this::toDto);
  }

  public String etagForWorkItemHistory(Long workItemId, Pageable pageable) {
    Objects.requireNonNull(workItemId, "workItemId");
    Objects.requireNonNull(pageable, "pageable");
    Object[] sig = repo.signatureWorkItem(workItemId);
    return buildEtag("w:" + workItemId, sig, pageable);
  }

  @Transactional(readOnly = true)
  public Page<UiHistoryEntryDto> historyByInboxKey(String inboxKey, Pageable pageable) {
    Page<UiStateHistory> p = repo.findByInboxKeyOrderByOccurredAtDesc(inboxKey, pageable);
    return p.map(this::toDto);
  }

  public String etagForInboxHistory(String inboxKey, Pageable pageable) {
    Objects.requireNonNull(inboxKey, "inboxKey");
    Objects.requireNonNull(pageable, "pageable");
    Object[] sig = repo.signatureInbox(inboxKey);
    return buildEtag("i:" + inboxKey, sig, pageable);
  }

  private static String buildEtag(String scope, Object[] sig, Pageable pageable) {
    Instant maxAt = sig != null && sig.length > 0 ? (Instant) sig[0] : null;
    long count = sig != null && sig.length > 1 && sig[1] != null ? ((Number) sig[1]).longValue() : 0L;

    String material = scope
        + "|p=" + pageable.getPageNumber() + "|s=" + pageable.getPageSize()
        + "|t=" + (maxAt == null ? "0" : maxAt.toEpochMilli())
        + "|c=" + count;

    return "W/\"" + sha256_16(material) + "\"";
  }

  private static String sha256_16(String material) {
    return Hashes.sha256HexPrefix(material, 32);
  }

  private UiHistoryEntryDto toDto(UiStateHistory e) {
    return UiHistoryEntryDto.builder()
        .id(e.getId())
        .subjectType(e.getSubjectType())
        .processoId(e.getProcessoId())
        .workItemId(e.getWorkItemId())
        .inboxKey(e.getInboxKey())
        .eventType(e.getEventType())
        .fromStatus(e.getFromStatus())
        .toStatus(e.getToStatus())
        .fromTokens(parseTokens(e.getFromTokensJson()))
        .toTokens(parseTokens(e.getToTokensJson()))
        .actorUserId(e.getActorUserId())
        .actorRole(e.getActorRole())
        .message(e.getMessage())
        .occurredAt(e.getOccurredAt())
        .build();
  }

  private void publishLive(UiStateHistory e, EnumSet<UiToken> from, EnumSet<UiToken> to) {
    List<String> topics = liveTopics(e);
    if (topics.isEmpty()) {
      return;
    }

    UiHistoryEntryDto dto = UiHistoryEntryDto.builder()
        .id(e.getId())
        .subjectType(e.getSubjectType())
        .processoId(e.getProcessoId())
        .workItemId(e.getWorkItemId())
        .inboxKey(e.getInboxKey())
        .eventType(e.getEventType())
        .fromStatus(e.getFromStatus())
        .toStatus(e.getToStatus())
        .fromTokens(tokensToList(from))
        .toTokens(tokensToList(to))
        .actorUserId(e.getActorUserId())
        .actorRole(e.getActorRole())
        .message(e.getMessage())
        .occurredAt(e.getOccurredAt())
        .build();

    for (String topic : topics) {
      outbox.enqueue(
          topic,
          OutboxPublisher.EVT_UI_HISTORY_LIVE,
          dto,
          PayloadMaps.ofEntries(
              "topic", topic,
              "subjectType", e.getSubjectType() != null ? e.getSubjectType().name() : null,
              "eventType", e.getEventType(),
              "processoId", e.getProcessoId(),
              "workItemId", e.getWorkItemId(),
              "inboxKey", normalizeNullable(e.getInboxKey())
          ),
          "uiHist:" + e.getId() + ":" + sha256_16(topic),
          "UI_HISTORY",
          String.valueOf(e.getId())
      );
    }
  }

  private UiHistoryEntryDto buildLiveDto(UiStateHistory e, EnumSet<UiToken> from, EnumSet<UiToken> to) {
    return UiHistoryEntryDto.builder()
        .id(e.getId())
        .subjectType(e.getSubjectType())
        .processoId(e.getProcessoId())
        .workItemId(e.getWorkItemId())
        .inboxKey(e.getInboxKey())
        .eventType(e.getEventType())
        .fromStatus(e.getFromStatus())
        .toStatus(e.getToStatus())
        .fromTokens(tokensToList(from))
        .toTokens(tokensToList(to))
        .actorUserId(e.getActorUserId())
        .actorRole(e.getActorRole())
        .message(e.getMessage())
        .occurredAt(e.getOccurredAt())
        .build();
  }

  private void publishCidadaoPanel(Processo processoAtual, UiStateHistory e, EnumSet<UiToken> from, EnumSet<UiToken> to) {
    if (processoAtual == null || e == null) return;

    String cpfAutor = processoAtual.getParteAutoraCpf();
    String cpfReu = processoAtual.getParteReuCpf();
    Set<String> cpfs = new LinkedHashSet<>();
    if (cpfAutor != null && !cpfAutor.isBlank()) cpfs.add(cpfAutor.trim());
    if (cpfReu != null && !cpfReu.isBlank()) cpfs.add(cpfReu.trim());
    if (cpfs.isEmpty()) return;

    UiHistoryEntryDto dto = buildLiveDto(e, from, to);
    for (String cpf : cpfs) {
      String routingKey = "HIST:INBOX:CIDCPF:" + cpf;
      outbox.enqueue(
          routingKey,
          OutboxPublisher.EVT_UI_HISTORY_LIVE,
          dto,
          PayloadMaps.ofEntries(
              "topic", routingKey,
              "subjectType", e.getSubjectType() != null ? e.getSubjectType().name() : null,
              "cidCpf", cpf,
              "processoId", processoAtual.getId(),
              "eventType", e.getEventType()
          ),
          "uiHistCid:" + e.getId() + ":" + cpf,
          "UI_HISTORY_CID",
          cpf
      );

      long t = Instant.now().getEpochSecond();
      String rk = "CIDASH:CPF:" + cpf;
      outbox.enqueue(
          rk,
          OutboxPublisher.EVT_CIDADAO_DASHBOARD_REFRESH,
          new CidadaoDashboardRefreshRequest(cpf, t),
          PayloadMaps.ofEntries("topic", rk, "cpf", cpf, "processoId", processoAtual.getId()),
          "dashRefresh:" + cpf + ":" + (t / 60L),
          "CIDASH",
          cpf
      );
    }
  }


  static List<String> liveTopics(UiStateHistory e) {
    if (e == null) {
      return List.of();
    }
    LinkedHashSet<String> topics = new LinkedHashSet<>();
    if (e.getSubjectType() == null) {
      addInboxTopic(topics, e.getInboxKey());
      addWorkItemTopic(topics, e.getWorkItemId());
      addProcessoTopic(topics, e.getProcessoId());
      return topics.isEmpty() ? List.of() : List.copyOf(topics);
    }
    switch (e.getSubjectType()) {
      case INBOX -> {
        addInboxTopic(topics, e.getInboxKey());
        addProcessoTopic(topics, e.getProcessoId());
        addWorkItemTopic(topics, e.getWorkItemId());
      }
      case WORKITEM -> {
        addWorkItemTopic(topics, e.getWorkItemId());
        addProcessoTopic(topics, e.getProcessoId());
        addInboxTopic(topics, e.getInboxKey());
      }
      case PROCESSO -> {
        addProcessoTopic(topics, e.getProcessoId());
        addInboxTopic(topics, e.getInboxKey());
        addWorkItemTopic(topics, e.getWorkItemId());
      }
      default -> {
        addInboxTopic(topics, e.getInboxKey());
        addWorkItemTopic(topics, e.getWorkItemId());
        addProcessoTopic(topics, e.getProcessoId());
      }
    }
    return topics.isEmpty() ? List.of() : List.copyOf(topics);
  }


  private static void addInboxTopic(Set<String> topics, String inboxKey) {
    String normalized = normalizeNullable(inboxKey);
    if (normalized != null) {
      topics.add("HIST:INBOX:" + normalized);
    }
  }

  private static void addProcessoTopic(Set<String> topics, Long processoId) {
    if (processoId != null) {
      topics.add("HIST:" + processoId);
    }
  }

  private static void addWorkItemTopic(Set<String> topics, Long workItemId) {
    if (workItemId != null) {
      topics.add("HIST:WORKITEM:" + workItemId);
    }
  }

  private static List<UiToken> tokensToList(EnumSet<UiToken> set) {
    if (set == null || set.isEmpty()) {
      return List.of();
    }
    return List.copyOf(set);
  }

  private String tokensJson(EnumSet<UiToken> tokens) {
    try {
      List<String> list = new ArrayList<>(tokens.size());
      for (UiToken t : tokens) {
        list.add(t.name());
      }
      return mapper.writeValueAsString(list);
    } catch (Exception ex) {
      return "[]";
    }
  }

  private List<UiToken> parseTokens(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      List<?> raw = mapper.readValue(json, List.class);
      List<UiToken> out = new ArrayList<>();
      for (Object o : raw) {
        if (o == null) continue;
        String s = String.valueOf(o).trim();
        if (s.isEmpty()) continue;
        try {
          out.add(UiToken.valueOf(s.toUpperCase(Locale.ROOT)));
        } catch (Exception ignored) {
        }
      }
      return List.copyOf(out);
    } catch (Exception ex) {
      return List.of();
    }
  }

  private static Long safeProcessoId(WorkItem w) {
    try {
      if (w.getProcesso() != null) {
        return w.getProcesso().getId();
      }
      return w.getProcessoId();
    } catch (Exception ignored) {
      return w.getProcessoId();
    }
  }

  private static String normalizeNullable(String v) {
    if (v == null) return null;
    String s = v.trim();
    return s.isEmpty() ? null : s;
  }

  private static Processo snapshot(Processo base, StatusProcesso status, String resultadoFinal) {
    Processo p = new Processo();
    p.setId(base.getId());
    p.setNivelSigilo(base.getNivelSigilo());
    p.setStatusProcesso(status);
    p.setResultadoFinal(resultadoFinal);
    return p;
  }
}
