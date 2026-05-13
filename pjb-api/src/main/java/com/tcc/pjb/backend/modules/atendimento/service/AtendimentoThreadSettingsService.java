package com.tcc.pjb.backend.modules.atendimento.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoThreadNotificationSettingsDto;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoUpdateThreadNotificationSettingsRequest;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThreadMemberSettings;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoThreadMemberSettingsRepository;

@Service
public class AtendimentoThreadSettingsService {

  
  private static final ZoneId ZONE = ZoneId.of("America/Fortaleza");

  private final CurrentUserService currentUser;
  private final AtendimentoChatService chat;
  private final AtendimentoThreadMemberSettingsRepository repo;

  public AtendimentoThreadSettingsService(CurrentUserService currentUser,
                                         AtendimentoChatService chat,
                                         AtendimentoThreadMemberSettingsRepository repo) {
    this.currentUser = Objects.requireNonNull(currentUser);
    this.chat = Objects.requireNonNull(chat);
    this.repo = Objects.requireNonNull(repo);
  }

  @Transactional(readOnly = true)
  public AtendimentoThreadNotificationSettingsDto get(Long threadId) {
    if (threadId == null) throw new IllegalArgumentException("threadId");
    chat.requireThreadAccess(threadId);
    long uid = currentUser.currentUserIdOrZero();
    Instant now = Instant.now();

    AtendimentoThreadMemberSettings s = repo.findByThreadIdAndUsuarioId(threadId, uid).orElse(null);
    boolean muted = isMutedNow(s, now);

    return new AtendimentoThreadNotificationSettingsDto(
        threadId,
        uid,
        s != null ? s.getMutedUntil() : null,
        s != null && s.getQuietHoursStartMin() != null ? Integer.valueOf(s.getQuietHoursStartMin()) : null,
        s != null && s.getQuietHoursEndMin() != null ? Integer.valueOf(s.getQuietHoursEndMin()) : null,
        s != null ? s.getQuietDaysMask() : null,
        muted,
        now
    );
  }

  @Transactional
  public AtendimentoThreadNotificationSettingsDto update(Long threadId, AtendimentoUpdateThreadNotificationSettingsRequest req) {
    if (threadId == null) throw new IllegalArgumentException("threadId");
    chat.requireThreadAccess(threadId);
    long uid = currentUser.currentUserIdOrZero();
    Instant now = Instant.now();

    AtendimentoThreadMemberSettings s = repo.findByThreadIdAndUsuarioId(threadId, uid)
        .orElseGet(() -> AtendimentoThreadMemberSettings.builder()
            .threadId(threadId)
            .usuarioId(uid)
            .createdAt(now)
            .updatedAt(now)
            .build());

    if (req != null) {
      
      s.setMutedUntil(req.mutedUntil());

      Short qs = normalizeMinutes(req.quietHoursStartMin());
      Short qe = normalizeMinutes(req.quietHoursEndMin());
      Integer mask = normalizeDaysMask(req.quietDaysMask());

      s.setQuietHoursStartMin(qs);
      s.setQuietHoursEndMin(qe);
      s.setQuietDaysMask(mask);
    }

    s.setUpdatedAt(now);
    repo.save(s);

    boolean muted = isMutedNow(s, now);
    return new AtendimentoThreadNotificationSettingsDto(
        threadId,
        uid,
        s.getMutedUntil(),
        s.getQuietHoursStartMin() != null ? Integer.valueOf(s.getQuietHoursStartMin()) : null,
        s.getQuietHoursEndMin() != null ? Integer.valueOf(s.getQuietHoursEndMin()) : null,
        s.getQuietDaysMask(),
        muted,
        now
    );
  }

  public boolean isMutedNow(Long threadId, Long usuarioId, Instant at) {
    if (threadId == null || usuarioId == null || at == null) return false;
    AtendimentoThreadMemberSettings s = repo.findByThreadIdAndUsuarioId(threadId, usuarioId).orElse(null);
    return isMutedNow(s, at);
  }

  public boolean isMutedNow(AtendimentoThreadMemberSettings s, Instant at) {
    if (s == null || at == null) return false;
    Instant mu = s.getMutedUntil();
    if (mu != null && at.isBefore(mu)) return true;
    return isQuietHoursActive(s, at);
  }

  public boolean isQuietHoursActive(AtendimentoThreadMemberSettings s, Instant at) {
    if (s == null || at == null) return false;
    Short start = s.getQuietHoursStartMin();
    Short end = s.getQuietHoursEndMin();
    Integer mask = s.getQuietDaysMask();
    if (start == null || end == null || mask == null) return false;

    ZonedDateTime z = at.atZone(ZONE);
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

  private static Short normalizeMinutes(Integer x) {
    if (x == null) return null;
    int v = x.intValue();
    if (v < 0 || v > 1439) throw new IllegalArgumentException("quietHours");
    return (short) v;
  }

  private static Integer normalizeDaysMask(Integer x) {
    if (x == null) return null;
    int v = x.intValue();
    
    if (v < 0 || v > 127) throw new IllegalArgumentException("quietDaysMask");
    return v;
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
}
