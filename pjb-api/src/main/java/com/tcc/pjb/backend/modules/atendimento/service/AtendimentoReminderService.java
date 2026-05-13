package com.tcc.pjb.backend.modules.atendimento.service;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.dto.ui.UiToken;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoCreateReminderRequest;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoReminderDto;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessage;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageStatus;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoReadState;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoReminder;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoReminderStatus;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThread;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoMessageRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoReadStateRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoReminderRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoThreadRepository;
import com.tcc.pjb.backend.service.ui.UiHistoryService;

@Service
public class AtendimentoReminderService {

  private final AtendimentoTosService tos;
  private final CurrentUserService currentUser;
  private final AtendimentoChatService chat;
  private final AtendimentoReminderRepository repo;
  private final AtendimentoThreadRepository threadRepo;
  private final AtendimentoMessageRepository messageRepo;
  private final AtendimentoReadStateRepository readRepo;
  private final AtendimentoInboxLiveHub liveHub;
  private final ObjectMapper mapper;
  private final UiHistoryService uiHistory;
  private final UsuarioRepository usuarioRepo;
  private final ProcessoRepository processoRepo;

  public AtendimentoReminderService(
      AtendimentoTosService tos,
      CurrentUserService currentUser,
      AtendimentoChatService chat,
      AtendimentoReminderRepository repo,
      AtendimentoThreadRepository threadRepo,
      AtendimentoMessageRepository messageRepo,
      AtendimentoReadStateRepository readRepo,
      AtendimentoInboxLiveHub liveHub,
      ObjectMapper mapper,
      UiHistoryService uiHistory,
      UsuarioRepository usuarioRepo,
      ProcessoRepository processoRepo
  ) {
    this.tos = Objects.requireNonNull(tos, "tos");
    this.currentUser = Objects.requireNonNull(currentUser, "currentUser");
    this.chat = Objects.requireNonNull(chat, "chat");
    this.repo = Objects.requireNonNull(repo, "repo");
    this.threadRepo = Objects.requireNonNull(threadRepo, "threadRepo");
    this.messageRepo = Objects.requireNonNull(messageRepo, "messageRepo");
    this.readRepo = Objects.requireNonNull(readRepo, "readRepo");
    this.liveHub = Objects.requireNonNull(liveHub, "liveHub");
    this.mapper = Objects.requireNonNull(mapper, "mapper");
    this.uiHistory = Objects.requireNonNull(uiHistory, "uiHistory");
    this.usuarioRepo = Objects.requireNonNull(usuarioRepo, "usuarioRepo");
    this.processoRepo = Objects.requireNonNull(processoRepo, "processoRepo");
  }

  @Transactional
  public AtendimentoReminderDto create(Long threadId, AtendimentoCreateReminderRequest req) {
    tos.requireAccepted();
    if (threadId == null) throw new IllegalArgumentException("threadId");
    if (req == null) throw new IllegalArgumentException("req");
    String body = req.body() == null ? null : req.body().trim();
    if (body == null || body.isBlank()) throw new IllegalArgumentException("body");
    if (body.length() > 1200) body = body.substring(0, 1200);

    Instant fireAt = req.fireAt();
    if (fireAt == null) throw new IllegalArgumentException("fireAt");
    Instant now = Instant.now();
    if (fireAt.isBefore(now.plusSeconds(30))) {
      throw new IllegalArgumentException("fireAt_too_soon");
    }
    if (fireAt.isAfter(now.plusSeconds(3600L * 24L * 365L))) {
      throw new IllegalArgumentException("fireAt_too_far");
    }

    AtendimentoThread t = chat.requireThreadAccess(threadId);
    Usuario actor = currentUser.getRequired();
    if (actor.getTipoUsuario() != TipoUsuario.ADVOGADO || !Objects.equals(t.getAdvogadoId(), actor.getId())) {
      throw new AccessDeniedException("apenas_advogado");
    }

    Long target = req.targetUserId();
    if (target == null) {
      target = t.getCidadaoUsuarioId();
    }
    if (!Objects.equals(target, t.getCidadaoUsuarioId()) && !Objects.equals(target, t.getAdvogadoId())) {
      throw new IllegalArgumentException("targetUserId");
    }

    AtendimentoReminder r = AtendimentoReminder.builder()
        .threadId(threadId)
        .createdByUserId(actor.getId())
        .targetUserId(target)
        .body(body)
        .fireAt(fireAt)
        .status(AtendimentoReminderStatus.PENDING)
        .attempts(0)
        .lastError(null)
        .sentMessageId(null)
        .createdAt(now)
        .updatedAt(now)
        .version(0L)
        .build();

    repo.save(r);
    return toDto(r);
  }

  @Transactional(readOnly = true)
  public Page<AtendimentoReminderDto> list(Long threadId, int page, int size) {
    tos.requireAccepted();
    if (threadId == null) throw new IllegalArgumentException("threadId");
    chat.requireThreadAccess(threadId);
    int p = Math.max(0, page);
    int s = Math.min(100, Math.max(1, size));
    Page<AtendimentoReminder> pg = repo.findByThreadIdOrderByFireAtDesc(threadId, PageRequest.of(p, s));
    if (pg.isEmpty()) {
      return new PageImpl<>(List.of(), PageRequest.of(p, s), 0);
    }
    List<AtendimentoReminderDto> out = pg.getContent().stream().map(AtendimentoReminderService::toDto).toList();
    return new PageImpl<>(out, pg.getPageable(), pg.getTotalElements());
  }

  @Transactional
  public void cancel(Long reminderId) {
    tos.requireAccepted();
    if (reminderId == null) throw new IllegalArgumentException("reminderId");
    AtendimentoReminder r = repo.findById(reminderId).orElseThrow();
    AtendimentoThread t = chat.requireThreadAccess(r.getThreadId());

    Usuario actor = currentUser.getRequired();
    boolean ok = Objects.equals(actor.getId(), r.getCreatedByUserId()) || Objects.equals(actor.getId(), t.getAdvogadoId());
    if (!ok) {
      throw new AccessDeniedException("sem_permissao");
    }

    repo.cancelAnyNotSent(reminderId, Instant.now());
  }

  
  @Scheduled(fixedDelayString = "${pjb.atendimento.reminders.pollMs:30000}")
  @Transactional
  public void dispatchDueReminders() {
    Instant now = Instant.now();
    List<AtendimentoReminder> due = repo.findByStatusAndFireAtLessThanEqualOrderByFireAtAsc(
        AtendimentoReminderStatus.PENDING,
        now,
        PageRequest.of(0, 50)
    );
    if (due.isEmpty()) {
      return;
    }

    for (AtendimentoReminder r : due) {
      try {
        if (r.getId() == null) continue;
        int claimed = repo.claim(r.getId(), now);
        if (claimed != 1) continue;
        AtendimentoReminder locked = repo.findById(r.getId()).orElse(null);
        if (locked == null) continue;
        deliverReminder(locked);
      } catch (Exception ex) {
        
        try {
          AtendimentoReminder fresh = repo.findById(r.getId()).orElse(null);
          if (fresh != null && fresh.getStatus() == AtendimentoReminderStatus.SENDING) {
            fresh.setAttempts(fresh.getAttempts() + 1);
            String msg = ex.getMessage();
            if (msg != null && msg.length() > 160) msg = msg.substring(0, 160);
            fresh.setLastError(msg);
            fresh.setStatus(AtendimentoReminderStatus.PENDING);
            fresh.setUpdatedAt(Instant.now());
            repo.save(fresh);
          }
        } catch (Exception ignored) {
        }
      }
    }
  }

  private void deliverReminder(AtendimentoReminder r) {
    if (r == null) return;
    if (r.getStatus() != AtendimentoReminderStatus.SENDING) return;
    AtendimentoThread t = threadRepo.findByIdForUpdate(r.getThreadId()).orElseThrow();

    Long senderId = r.getCreatedByUserId();
    if (senderId == null) throw new IllegalStateException("senderId");
    if (!Objects.equals(t.getAdvogadoId(), senderId)) {
      
      throw new IllegalStateException("reminder_sender_not_advogado");
    }

    Long target = r.getTargetUserId();
    if (target == null) {
      target = t.getCidadaoUsuarioId();
    }

    Instant now = Instant.now();
    String prevHash = messageRepo.findTopByThreadIdOrderByIdDesc(t.getId()).map(AtendimentoMessage::getMsgHash).orElse(null);
    String senderTipo = "ADVOGADO";
    String body = "LEMBRETE: " + safeBody(r.getBody());
    String msgHash = computeMsgHash(prevHash, t.getId(), senderId, senderTipo, now, body);

    AtendimentoMessage m = messageRepo.save(AtendimentoMessage.builder()
        .threadId(t.getId())
        .senderUsuarioId(senderId)
        .senderTipo(senderTipo)
        .body(body)
        .status(AtendimentoMessageStatus.DELIVERED)
        .prevHash(prevHash)
        .msgHash(msgHash)
        .createdAt(now)
        .build());

    t.setUpdatedAt(now);
    t.setLastMessageId(m.getId());
    threadRepo.save(t);

    upsertRead(t.getId(), senderId, m.getId(), now);

    publishLiveInboxEvent(t, m);
    publishUiNotificationReminder(t, m, senderId, target);

    r.setStatus(AtendimentoReminderStatus.SENT);
    r.setSentMessageId(m.getId());
    r.setUpdatedAt(now);
    repo.save(r);
  }

  private void publishLiveInboxEvent(AtendimentoThread t, AtendimentoMessage m) {
    String json;
    try {
      json = mapper.writeValueAsString(PayloadMaps.ofEntries(
          "type", "ATENDIMENTO_NEW_MESSAGE",
          "threadId", t.getId(),
          "processoId", t.getProcessoId(),
          "messageId", m.getId(),
          "at", m.getCreatedAt() != null ? m.getCreatedAt().toString() : null
      ));
    } catch (Exception e) {
      return;
    }
    liveHub.enqueue("atendimento:uid:" + t.getAdvogadoId(), json);
    liveHub.enqueue("atendimento:uid:" + t.getCidadaoUsuarioId(), json);
  }

  private void publishUiNotificationReminder(AtendimentoThread t, AtendimentoMessage m, Long senderId, Long recipientUserId) {
    if (t == null || recipientUserId == null) return;

    Usuario recipient = usuarioRepo.findById(recipientUserId).orElse(null);
    String inboxKey;
    if (recipient != null && recipient.getTipoUsuario() == TipoUsuario.CIDADAO) {
      String cpf = recipient.getCpf();
      inboxKey = (cpf != null && !cpf.isBlank()) ? ("CIDCPF:" + cpf.trim()) : ("USR:" + recipientUserId);
    } else {
      inboxKey = "USR:" + recipientUserId;
    }

    Processo pr = processoRepo.findById(t.getProcessoId()).orElse(null);
    String numero = pr != null ? pr.getNumeroUnificado() : null;
    String msg = "NOTIFICADO: lembrete do advogado" + (numero == null ? "" : " • Processo " + numero);
    try {
      uiHistory.recordInboxEvent(
          inboxKey,
          t.getProcessoId(),
          UiHistoryService.EVT_ATENDIMENTO_REMINDER,
          EnumSet.of(UiToken.NOTIFICADO, UiToken.URGENTE),
          senderId,
          "ADVOGADO",
          msg
      );
    } catch (Exception ignored) {
    }
  }

  private void upsertRead(Long threadId, Long usuarioId, Long lastReadMessageId, Instant at) {
    if (threadId == null || usuarioId == null || lastReadMessageId == null) return;
    AtendimentoReadState st = readRepo.findByThreadIdAndUsuarioId(threadId, usuarioId).orElse(null);
    if (st == null) {
      readRepo.save(AtendimentoReadState.builder()
          .threadId(threadId)
          .usuarioId(usuarioId)
          .lastReadMessageId(lastReadMessageId)
          .updatedAt(at)
          .build());
      return;
    }
    Long cur = st.getLastReadMessageId();
    if (cur == null || cur.longValue() < lastReadMessageId.longValue()) {
      st.setLastReadMessageId(lastReadMessageId);
      st.setUpdatedAt(at);
      readRepo.save(st);
    }
  }

  private static String computeMsgHash(String prevHash,
                                       Long threadId,
                                       Long senderId,
                                       String senderTipo,
                                       Instant at,
                                       String body) {
    String p = prevHash != null ? prevHash : "";
    String b = body != null ? body : "";
    String raw = p + "|" + threadId + "|" + senderId + "|" + (senderTipo == null ? "" : senderTipo) + "|" + at.toEpochMilli() + "|" + b + "||";
    return Hashes.sha256Hex(raw);
  }

  private static String safeBody(String body) {
    if (body == null) return "";
    String s = body.replaceAll("[\\r\\n\\t]", " ").trim();
    if (s.length() > 1200) s = s.substring(0, 1200);
    return s;
  }

  private static AtendimentoReminderDto toDto(AtendimentoReminder r) {
    return new AtendimentoReminderDto(
        r.getId(),
        r.getThreadId(),
        r.getCreatedByUserId(),
        r.getTargetUserId(),
        r.getBody(),
        r.getFireAt(),
        r.getStatus() == null ? null : r.getStatus().name(),
        r.getAttempts(),
        r.getLastError(),
        r.getSentMessageId(),
        r.getCreatedAt(),
        r.getUpdatedAt()
    );
  }
}
