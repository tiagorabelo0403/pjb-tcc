package com.tcc.pjb.backend.modules.atendimento.service;

import java.time.Instant;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.modules.atendimento.model.AtendimentoChecklistItemStatus;
import com.tcc.pjb.backend.modules.atendimento.model.ChecklistThreadAgg;
import com.tcc.pjb.backend.modules.atendimento.util.ChecklistBadgeUtils;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoThreadDigestDto;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessage;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoReadState;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThread;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThreadMemberSettings;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoMessageRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoReadStateRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoChecklistItemRepository;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoThreadMemberSettingsRepository;

@Service
public class AtendimentoThreadDigestService {

  private final CurrentUserService currentUser;
  private final Clock clock;
  private final AtendimentoChatService chat;
  private final AtendimentoMessageRepository messageRepo;
  private final AtendimentoReadStateRepository readRepo;
  private final AtendimentoChecklistItemRepository checklistItemRepo;
  private final AtendimentoThreadPolicyService policy;
  private final AtendimentoThreadSettingsService settings;
  private final AtendimentoThreadMemberSettingsRepository settingsRepo;

  private final boolean attachmentsEnabled;
  private final long attachmentMaxBytes;
  private final int attachmentMaxPerMessage;

  public AtendimentoThreadDigestService(CurrentUserService currentUser,
                                       Clock clock,
                                       AtendimentoChatService chat,
                                       AtendimentoMessageRepository messageRepo,
                                       AtendimentoReadStateRepository readRepo,
                                       AtendimentoChecklistItemRepository checklistItemRepo,
                                       AtendimentoThreadPolicyService policy,
                                       AtendimentoThreadSettingsService settings,
                                       AtendimentoThreadMemberSettingsRepository settingsRepo,
                                       @Value("${pjb.atendimento.attachments.enabled:false}") boolean attachmentsEnabled,
                                       @Value("${pjb.atendimento.attachments.maxBytes:10485760}") long attachmentMaxBytes,
                                       @Value("${pjb.atendimento.attachments.maxPerMessage:3}") int attachmentMaxPerMessage) {
    this.currentUser = Objects.requireNonNull(currentUser);
    this.clock = Objects.requireNonNull(clock);
    this.chat = Objects.requireNonNull(chat);
    this.messageRepo = Objects.requireNonNull(messageRepo);
    this.readRepo = Objects.requireNonNull(readRepo);
    this.checklistItemRepo = Objects.requireNonNull(checklistItemRepo);
    this.policy = Objects.requireNonNull(policy);
    this.settings = Objects.requireNonNull(settings);
    this.settingsRepo = Objects.requireNonNull(settingsRepo);
    this.attachmentsEnabled = attachmentsEnabled;
    this.attachmentMaxBytes = attachmentMaxBytes;
    this.attachmentMaxPerMessage = Math.max(0, attachmentMaxPerMessage);
  }

  @Transactional(readOnly = true)
  public AtendimentoThreadDigestDto digest(Long threadId) {
    if (threadId == null) throw new IllegalArgumentException("threadId");
    AtendimentoThread t = chat.requireThreadAccess(threadId);
    Usuario u = currentUser.getRequired();
    Instant now = Instant.now(clock);

    Long lastMsgId = t.getLastMessageId();
    Instant lastAt = t.getUpdatedAt();
    String preview = null;
    if (lastMsgId != null) {
      AtendimentoMessage m = messageRepo.findById(lastMsgId).orElse(null);
      if (m != null) {
        preview = safePreview(m.getBody());
      }
    }

    boolean hasUnread = false;
    if (lastMsgId != null) {
      AtendimentoReadState st = readRepo.findByThreadIdAndUsuarioId(threadId, u.getId()).orElse(null);
      Long lr = st != null ? st.getLastReadMessageId() : null;
      hasUnread = lr == null || lr.longValue() < lastMsgId.longValue();
    }

    Instant disUntil = policy.citizenSendDisabledUntil(threadId);
    boolean citizenDisabledNow = u.getTipoUsuario() == TipoUsuario.CIDADAO && disUntil != null && now.isBefore(disUntil);

    boolean mutedNow = settings.isMutedNow(threadId, u.getId(), now);
    Instant mutedUntil = settingsRepo.findByThreadIdAndUsuarioId(threadId, u.getId())
        .map(AtendimentoThreadMemberSettings::getMutedUntil)
        .orElse(null);

    ChecklistThreadAgg agg = safeChecklistAgg(threadId, now);

    return new AtendimentoThreadDigestDto(
        threadId,
        t.getProcessoId(),
        u.getId(),
        hasUnread,
        lastMsgId,
        lastAt,
        preview,
        citizenDisabledNow,
        disUntil,
        mutedNow,
        mutedUntil,
        attachmentsEnabled,
        attachmentMaxBytes,
        attachmentMaxPerMessage,
        agg.openCount(),
        agg.overdueCount(),
        agg.nextDueAt(),
        ChecklistBadgeUtils.computeNextDueInMinutes(now, agg.nextDueAt()),
        ChecklistBadgeUtils.computeOverdueSinceMinutes(now, agg),
        now
    );
  }

  private static String safePreview(String body) {
    if (body == null) return null;
    String b = body.replaceAll("[\\r\\n\\t]+", " ").trim();
    if (b.isBlank()) return null;
    if (b.length() > 140) return b.substring(0, 140);
    return b;
  }

  private ChecklistThreadAgg safeChecklistAgg(Long threadId, Instant now) {
    if (threadId == null) return ChecklistThreadAgg.empty();
    Instant ref = now != null ? now : Instant.now(clock);

    try {
      List<AtendimentoChecklistItemRepository.ThreadChecklistAgg> rows =
          checklistItemRepo.aggregateByThreadIds(List.of(threadId), AtendimentoChecklistItemStatus.OPEN, ref);
      if (rows == null || rows.isEmpty()) return ChecklistThreadAgg.empty();
      AtendimentoChecklistItemRepository.ThreadChecklistAgg r = rows.get(0);
      if (r == null) return ChecklistThreadAgg.empty();
      int open = clampToInt(r.getOpenCnt());
      int overdue = clampToInt(r.getOverdueCnt());
      return new ChecklistThreadAgg(open, overdue, r.getNextDueAt(), r.getOldestOverdueAt());
    } catch (Exception e) {
      return ChecklistThreadAgg.empty();
    }
  }

  private static int clampToInt(long v) {
    if (v <= 0) return 0;
    if (v >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
    return (int) v;
  }

}
