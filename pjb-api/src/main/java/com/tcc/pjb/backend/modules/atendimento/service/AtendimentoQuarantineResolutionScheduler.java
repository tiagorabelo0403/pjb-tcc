package com.tcc.pjb.backend.modules.atendimento.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AtendimentoQuarantineResolutionScheduler {

  private final AtendimentoMessageRepository messageRepo;
  private final AtendimentoMessageAttachmentRepository msgAttRepo;
  private final AtendimentoAttachmentRepository attachmentRepo;
  private final AtendimentoThreadRepository threadRepo;
  private final UsuarioRepository usuarioRepo;
  private final AtendimentoInboxLiveHub liveHub;
  private final AtendimentoModerationEventService modEvents;
  private final ObjectMapper mapper;
  private final long timeoutMs;

  public AtendimentoQuarantineResolutionScheduler(AtendimentoMessageRepository messageRepo,
                                                 AtendimentoMessageAttachmentRepository msgAttRepo,
                                                 AtendimentoAttachmentRepository attachmentRepo,
                                                 AtendimentoThreadRepository threadRepo,
                                                 UsuarioRepository usuarioRepo,
                                                 AtendimentoInboxLiveHub liveHub,
                                                 AtendimentoModerationEventService modEvents,
                                                 ObjectMapper mapper,
                                                 @Value("${pjb.atendimento.quarantine.timeoutMs:86400000}") long timeoutMs) {
    this.messageRepo = Objects.requireNonNull(messageRepo);
    this.msgAttRepo = Objects.requireNonNull(msgAttRepo);
    this.attachmentRepo = Objects.requireNonNull(attachmentRepo);
    this.threadRepo = Objects.requireNonNull(threadRepo);
    this.usuarioRepo = Objects.requireNonNull(usuarioRepo);
    this.liveHub = Objects.requireNonNull(liveHub);
    this.modEvents = Objects.requireNonNull(modEvents);
    this.mapper = Objects.requireNonNull(mapper);
    this.timeoutMs = timeoutMs;
  }

  @Scheduled(fixedDelayString = "${pjb.atendimento.quarantine.resolveMs:20000}")
  @Transactional
  public void run() {
    List<AtendimentoMessage> candidates = messageRepo.findTop200ByStatusOrderByIdAsc(AtendimentoMessageStatus.QUARANTINED);
    if (candidates.isEmpty()) return;

    List<Long> msgIds = candidates.stream().map(AtendimentoMessage::getId).filter(Objects::nonNull).toList();
    Map<Long, List<Long>> attIdsByMsg = new HashMap<>();
    for (AtendimentoMessageAttachment ma : msgAttRepo.findByMessageIds(msgIds)) {
      attIdsByMsg.computeIfAbsent(ma.getId().getMessageId(), k -> new ArrayList<>()).add(ma.getId().getAttachmentId());
    }

    Set<Long> allAttIds = attIdsByMsg.values().stream().flatMap(List::stream).collect(Collectors.toSet());
    Map<Long, AtendimentoAttachment> attMap = allAttIds.isEmpty() ? Map.of() : attachmentRepo.findAllById(allAttIds).stream().collect(Collectors.toMap(AtendimentoAttachment::getId, x -> x));

    Instant now = Instant.now();

    for (AtendimentoMessage m : candidates) {
      List<Long> ids = attIdsByMsg.getOrDefault(m.getId(), List.of());
      if (ids.isEmpty()) {
        block(m, "attachment_missing", now);
        continue;
      }

      boolean hasRejected = false;
      boolean hasExpired = false;
      boolean allReady = true;

      for (Long id : ids) {
        AtendimentoAttachment a = attMap.get(id);
        if (a == null) {
          allReady = false;
          continue;
        }
        if (a.getStatus() == AtendimentoAttachmentStatus.REJECTED) {
          hasRejected = true;
          allReady = false;
        } else if (a.getStatus() == AtendimentoAttachmentStatus.EXPIRED) {
          hasExpired = true;
          allReady = false;
        } else if (a.getStatus() != AtendimentoAttachmentStatus.READY) {
          allReady = false;
        }
      }

      if (allReady) continue;

      if (hasRejected) {
        block(m, "attachment_rejected", now);
        continue;
      }

      if (hasExpired) {
        block(m, "attachment_expired", now);
        continue;
      }

      if (m.getCreatedAt() != null && now.toEpochMilli() - m.getCreatedAt().toEpochMilli() > timeoutMs) {
        block(m, "attachment_timeout", now);
      }
    }
  }

  private void block(AtendimentoMessage m, String reason, Instant now) {
    if (m.getStatus() == AtendimentoMessageStatus.BLOCKED) return;
    m.setStatus(AtendimentoMessageStatus.BLOCKED);
    m.setBlockedReason(reason);
    m.setBlockedAt(now);
    messageRepo.save(m);

    AtendimentoThread t = threadRepo.findById(m.getThreadId()).orElse(null);
    if (t != null) {
      t.setUpdatedAt(now);
      threadRepo.save(t);
      Usuario actor = usuarioRepo.findById(m.getSenderUsuarioId()).orElse(null);
      if (actor != null) {
        modEvents.recordSystemBlock(actor, t, reason, Map.of("messageId", m.getId()));
      }
      publishBlockedToSender(t, m, reason);
    }
  }

  private void publishBlockedToSender(AtendimentoThread t, AtendimentoMessage m, String reason) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("type", "ATENDIMENTO_MESSAGE_BLOCKED");
    payload.put("threadId", t.getId());
    payload.put("messageId", m.getId());
    payload.put("reason", reason);
    payload.put("at", Instant.now().toString());

    String json;
    try {
      json = mapper.writeValueAsString(payload);
    } catch (Exception e) {
      return;
    }
    liveHub.enqueue("ATEND:USR:" + m.getSenderUsuarioId(), json);
  }
}
