package com.tcc.pjb.backend.modules.atendimento.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoAttachmentDto;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoModerationActionRequest;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoModerationMessageDetailDto;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoModerationQueueItemDto;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoAttachment;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoAttachmentStatus;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessage;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageAttachment;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageStatus;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThread;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoAttachmentRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoMessageAttachmentRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoMessageRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoThreadRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtendimentoModerationService {

  private final CurrentUserService currentUser;
  private final AtendimentoThreadRepository threadRepo;
  private final AtendimentoMessageRepository messageRepo;
  private final AtendimentoMessageAttachmentRepository msgAttRepo;
  private final AtendimentoAttachmentRepository attachmentRepo;
  private final ProcessoRepository processoRepo;
  private final UsuarioRepository usuarioRepo;
  private final AtendimentoInboxLiveHub liveHub;
  private final AtendimentoModerationEventService modEvents;
  private final ObjectMapper mapper;

  public AtendimentoModerationService(CurrentUserService currentUser,
                                     AtendimentoThreadRepository threadRepo,
                                     AtendimentoMessageRepository messageRepo,
                                     AtendimentoMessageAttachmentRepository msgAttRepo,
                                     AtendimentoAttachmentRepository attachmentRepo,
                                     ProcessoRepository processoRepo,
                                     UsuarioRepository usuarioRepo,
                                     AtendimentoInboxLiveHub liveHub,
                                     AtendimentoModerationEventService modEvents,
                                     ObjectMapper mapper) {
    this.currentUser = Objects.requireNonNull(currentUser);
    this.threadRepo = Objects.requireNonNull(threadRepo);
    this.messageRepo = Objects.requireNonNull(messageRepo);
    this.msgAttRepo = Objects.requireNonNull(msgAttRepo);
    this.attachmentRepo = Objects.requireNonNull(attachmentRepo);
    this.processoRepo = Objects.requireNonNull(processoRepo);
    this.usuarioRepo = Objects.requireNonNull(usuarioRepo);
    this.liveHub = Objects.requireNonNull(liveHub);
    this.modEvents = Objects.requireNonNull(modEvents);
    this.mapper = Objects.requireNonNull(mapper);
  }

  @Transactional(readOnly = true)
  public Page<AtendimentoModerationQueueItemDto> listQueue(List<String> statuses, Long cursor, int limit) {
    Usuario reviewer = currentUser.getRequired();
    if (!isInstitutional(reviewer)) throw new AccessDeniedException("Acesso negado");

    List<AtendimentoMessageStatus> st = parseStatuses(statuses);
    int size = Math.min(Math.max(limit, 1), 100);
    Pageable p = PageRequest.of(0, size);

    Page<AtendimentoMessage> page = messageRepo.findQueue(st, cursor, p);
    if (page.isEmpty()) return new PageImpl<>(List.of(), p, 0);

    List<AtendimentoMessage> messages = page.getContent();
    Set<Long> threadIds = messages.stream().map(AtendimentoMessage::getThreadId).collect(Collectors.toSet());
    Map<Long, AtendimentoThread> threads = threadRepo.findAllById(threadIds).stream().collect(Collectors.toMap(AtendimentoThread::getId, x -> x));

    Set<Long> processoIds = threads.values().stream().map(AtendimentoThread::getProcessoId).filter(Objects::nonNull).collect(Collectors.toSet());
    Map<Long, Processo> processos = processoIds.isEmpty() ? Map.of() : processoRepo.findAllById(processoIds).stream().collect(Collectors.toMap(Processo::getId, x -> x));

    List<Long> msgIds = messages.stream().map(AtendimentoMessage::getId).toList();
    Map<Long, List<Long>> attIdsByMsg = new HashMap<>();
    for (AtendimentoMessageAttachment ma : msgAttRepo.findByMessageIds(msgIds)) {
      attIdsByMsg.computeIfAbsent(ma.getId().getMessageId(), k -> new ArrayList<>()).add(ma.getId().getAttachmentId());
    }

    Set<Long> allAttIds = attIdsByMsg.values().stream().flatMap(List::stream).collect(Collectors.toSet());
    Map<Long, AtendimentoAttachment> attMap = allAttIds.isEmpty() ? Map.of() : attachmentRepo.findAllById(allAttIds).stream().collect(Collectors.toMap(AtendimentoAttachment::getId, x -> x));

    List<AtendimentoModerationQueueItemDto> out = new ArrayList<>(messages.size());
    for (AtendimentoMessage m : messages) {
      AtendimentoThread t = threads.get(m.getThreadId());
      Processo pr = t != null ? processos.get(t.getProcessoId()) : null;

      int total = 0;
      int ready = 0;
      int pending = 0;
      int rejected = 0;
      for (Long aid : attIdsByMsg.getOrDefault(m.getId(), List.of())) {
        AtendimentoAttachment a = attMap.get(aid);
        if (a == null) continue;
        total++;
        if (a.getStatus() == AtendimentoAttachmentStatus.READY) ready++;
        else if (a.getStatus() == AtendimentoAttachmentStatus.PENDING_SCAN) pending++;
        else if (a.getStatus() == AtendimentoAttachmentStatus.REJECTED || a.getStatus() == AtendimentoAttachmentStatus.EXPIRED) rejected++;
      }

      out.add(new AtendimentoModerationQueueItemDto(
          m.getId(),
          m.getThreadId(),
          t != null ? t.getProcessoId() : null,
          pr != null ? pr.getNumeroUnificado() : null,
          m.getSenderUsuarioId(),
          m.getSenderTipo(),
          m.getStatus() != null ? m.getStatus().name() : null,
          m.getCreatedAt(),
          m.getBlockedReason(),
          m.getBlockedAt(),
          total,
          ready,
          pending,
          rejected
      ));
    }

    return new PageImpl<>(out, p, page.getTotalElements());
  }

  @Transactional(readOnly = true)
  public AtendimentoModerationMessageDetailDto messageDetail(Long messageId) {
    Usuario reviewer = currentUser.getRequired();
    if (!isInstitutional(reviewer)) throw new AccessDeniedException("Acesso negado");

    AtendimentoMessage m = messageRepo.findById(messageId).orElseThrow();
    AtendimentoThread t = threadRepo.findById(m.getThreadId()).orElseThrow();
    Processo pr = processoRepo.findById(t.getProcessoId()).orElse(null);

    List<AtendimentoAttachment> atts = attachmentsForMessage(m.getId());
    List<AtendimentoAttachmentDto> a = atts.stream().map(AtendimentoAttachmentService::toDto).toList();

    return new AtendimentoModerationMessageDetailDto(
        m.getId(),
        m.getThreadId(),
        t.getProcessoId(),
        pr != null ? pr.getNumeroUnificado() : null,
        m.getSenderUsuarioId(),
        m.getSenderTipo(),
        m.getStatus() != null ? m.getStatus().name() : null,
        m.getBody(),
        m.getCreatedAt(),
        m.getMsgHash(),
        m.getPrevHash(),
        m.getBlockedReason(),
        m.getBlockedNote(),
        m.getBlockedAt(),
        m.getBlockedByUserId(),
        a
    );
  }

  @Transactional
  public void blockMessage(Long messageId, AtendimentoModerationActionRequest req) {
    Usuario reviewer = currentUser.getRequired();
    if (!isInstitutional(reviewer)) throw new AccessDeniedException("Acesso negado");

    AtendimentoMessage m = messageRepo.findById(messageId).orElseThrow();
    AtendimentoThread t = threadRepo.findById(m.getThreadId()).orElseThrow();

    m.setStatus(AtendimentoMessageStatus.BLOCKED);
    m.setBlockedReason(req != null ? req.reason() : "moderator_block");
    m.setBlockedNote(req != null ? truncate(req.note(), 200) : null);
    m.setBlockedAt(Instant.now());
    m.setBlockedByUserId(reviewer.getId());
    messageRepo.save(m);

    t.setUpdatedAt(Instant.now());
    threadRepo.save(t);

    modEvents.recordModeratorAction(reviewer, t, m.getBlockedReason(), PayloadMaps.ofEntries("messageId", m.getId()));

    publishToUser(t.getCidadaoUsuarioId(), PayloadMaps.ofEntries("type", "ATENDIMENTO_MESSAGE_BLOCKED", "threadId", t.getId(), "messageId", m.getId(), "reason", m.getBlockedReason(), "at", Instant.now().toString()));
    publishToUser(t.getAdvogadoId(), PayloadMaps.ofEntries("type", "ATENDIMENTO_MESSAGE_BLOCKED", "threadId", t.getId(), "messageId", m.getId(), "reason", m.getBlockedReason(), "at", Instant.now().toString()));
  }

  @Transactional
  public void releaseMessage(Long messageId) {
    Usuario reviewer = currentUser.getRequired();
    if (!isInstitutional(reviewer)) throw new AccessDeniedException("Acesso negado");

    AtendimentoMessage m = messageRepo.findById(messageId).orElseThrow();
    AtendimentoThread t = threadRepo.findById(m.getThreadId()).orElseThrow();

    List<AtendimentoAttachment> atts = attachmentsForMessage(m.getId());
    boolean allReady = atts.stream().allMatch(a -> a.getStatus() == AtendimentoAttachmentStatus.READY);
    if (!atts.isEmpty() && !allReady) {
      throw new IllegalArgumentException("attachments_not_ready");
    }

    m.setStatus(AtendimentoMessageStatus.DELIVERED);
    m.setBlockedReason(null);
    m.setBlockedNote(null);
    m.setBlockedAt(null);
    m.setBlockedByUserId(null);
    messageRepo.save(m);

    Instant now = Instant.now();
    t.setUpdatedAt(now);
    if (t.getLastMessageId() == null || t.getLastMessageId() < m.getId()) {
      t.setLastMessageId(m.getId());
    }
    threadRepo.save(t);

    modEvents.recordModeratorAction(reviewer, t, "moderator_release", PayloadMaps.ofEntries("messageId", m.getId()));

    publishToUser(t.getAdvogadoId(), PayloadMaps.ofEntries("type", "ATENDIMENTO_NEW_MESSAGE", "threadId", t.getId(), "processoId", t.getProcessoId(), "messageId", m.getId(), "at", now.toString()));
    publishToUser(t.getCidadaoUsuarioId(), PayloadMaps.ofEntries("type", "ATENDIMENTO_NEW_MESSAGE", "threadId", t.getId(), "processoId", t.getProcessoId(), "messageId", m.getId(), "at", now.toString()));
  }

  private List<AtendimentoAttachment> attachmentsForMessage(Long messageId) {
    List<AtendimentoMessageAttachment> links = msgAttRepo.findByMessageIds(List.of(messageId));
    if (links.isEmpty()) return List.of();
    List<Long> ids = links.stream().map(x -> x.getId().getAttachmentId()).toList();
    return attachmentRepo.findAllById(ids);
  }

  private void publishToUser(Long userId, Map<String, Object> payload) {
    if (userId == null) return;
    String json;
    try {
      json = mapper.writeValueAsString(payload);
    } catch (Exception e) {
      return;
    }
    liveHub.enqueue("ATEND:USR:" + userId, json);
  }

  private static List<AtendimentoMessageStatus> parseStatuses(List<String> raw) {
    if (raw == null || raw.isEmpty()) {
      return List.of(AtendimentoMessageStatus.QUARANTINED, AtendimentoMessageStatus.BLOCKED);
    }
    List<AtendimentoMessageStatus> out = new ArrayList<>();
    for (String s : raw) {
      if (s == null || s.isBlank()) continue;
      try {
        out.add(AtendimentoMessageStatus.valueOf(s.trim().toUpperCase()));
      } catch (Exception ignored) {
      }
    }
    if (out.isEmpty()) {
      out.add(AtendimentoMessageStatus.QUARANTINED);
      out.add(AtendimentoMessageStatus.BLOCKED);
    }
    return out;
  }

  private static boolean isInstitutional(Usuario u) {
    TipoUsuario t = u != null ? u.getTipoUsuario() : null;
    if (t == null) return false;
    return t.isAdmin() || t.isMagistratura() || t.isServidorJudiciario();
  }

  private static String truncate(String s, int max) {
    if (s == null) return null;
    String x = s.trim();
    if (x.isEmpty()) return null;
    return x.length() <= max ? x : x.substring(0, max);
  }
}
