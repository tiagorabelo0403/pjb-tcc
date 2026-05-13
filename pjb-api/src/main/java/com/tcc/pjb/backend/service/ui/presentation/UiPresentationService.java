package com.tcc.pjb.backend.service.ui.presentation;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ui.UiTheme;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityFlag;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityPreset;
import com.tcc.pjb.backend.model.dto.ui.presentation.UiPresentationBundleDto;
import com.tcc.pjb.backend.model.dto.ui.presentation.UiPresentationDto;
import com.tcc.pjb.backend.model.dto.ui.presentation.UiReadingIntensity;
import com.tcc.pjb.backend.model.dto.ui.presentation.UiReadingPreferenceDto;
import com.tcc.pjb.backend.model.dto.ui.presentation.UiReadingPreferenceUpdateRequestDto;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.ui.UsuarioAccessibilityPreference;
import com.tcc.pjb.backend.platform.hash.CanonicalJsonHasher;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.service.ui.preferences.UiUserPreferenceService;
import com.tcc.pjb.backend.service.ui.presentation.compiler.UiPresentationCompiler;
import com.tcc.pjb.backend.service.ui.presentation.compiler.UiCssTokenKey;

@Service
public class UiPresentationService {

  private final CurrentUserService currentUser;
  private final UiUserPreferenceService prefs;
  private final ReadingModeProperties reading;
  private final CanonicalJsonHasher hasher;
  private final ObjectMapper mapper;
  private final AuditLedgerService audit;
  private final OutboxPublisher outbox;
  private final UsuarioRepository usuarioRepo;

  private final boolean atendimentoAttachmentsEnabled;
  private final long atendimentoAttachmentMaxBytes;
  private final int atendimentoAttachmentMaxPerMessage;

  private final UiPresentationCompiler compiler;

  public UiPresentationService(
      CurrentUserService currentUser,
      UiUserPreferenceService prefs,
      ReadingModeProperties reading,
      CanonicalJsonHasher hasher,
      ObjectMapper mapper,
      AuditLedgerService audit,
      OutboxPublisher outbox,
      UsuarioRepository usuarioRepo,
      @Value("${pjb.atendimento.attachments.enabled:false}") boolean atendimentoAttachmentsEnabled,
      @Value("${pjb.atendimento.attachments.maxBytes:10485760}") long atendimentoAttachmentMaxBytes,
      @Value("${pjb.atendimento.attachments.maxPerMessage:3}") int atendimentoAttachmentMaxPerMessage
  ) {
    this.currentUser = Objects.requireNonNull(currentUser, "currentUser");
    this.prefs = Objects.requireNonNull(prefs, "prefs");
    this.reading = Objects.requireNonNull(reading, "reading");
    this.hasher = Objects.requireNonNull(hasher, "hasher");
    this.mapper = Objects.requireNonNull(mapper, "mapper");
    this.audit = Objects.requireNonNull(audit, "audit");
    this.outbox = Objects.requireNonNull(outbox, "outbox");
    this.usuarioRepo = Objects.requireNonNull(usuarioRepo, "usuarioRepo");
    this.atendimentoAttachmentsEnabled = atendimentoAttachmentsEnabled;
    this.atendimentoAttachmentMaxBytes = Math.max(0L, atendimentoAttachmentMaxBytes);
    this.atendimentoAttachmentMaxPerMessage = Math.max(0, atendimentoAttachmentMaxPerMessage);
    this.compiler = new UiPresentationCompiler(reading);
  }

  @Transactional
  public UiPresentationBundleDto bundleForCurrentUser() {
    Usuario u = currentUser.getRequired();
    return bundleForUserId(u.getId());
  }

  @Transactional
  public UiPresentationBundleDto bundleForUserId(long usuarioId) {
    UsuarioAccessibilityPreference pref = prefs.loadOrCreate(usuarioId);
    Instant now = Instant.now();
    UiPresentationDto light = compile(pref, UiTheme.LIGHT, now, usuarioId);
    UiPresentationDto dark = compile(pref, UiTheme.DARK, now, usuarioId);
    return new UiPresentationBundleDto(light, dark, now);
  }

  @Transactional
  public UiReadingPreferenceDto readingPreference() {
    Usuario u = currentUser.getRequired();
    UsuarioAccessibilityPreference pref = prefs.loadOrCreate(u.getId());
    UiReadingIntensity intensity = pref.getReadingIntensity() == null ? reading.getDefaultIntensity() : pref.getReadingIntensity();
    return new UiReadingPreferenceDto(pref.isReadingModeEnabled(), intensity, pref.getUpdatedAt());
  }

  @Transactional
  public UiReadingPreferenceDto updateReadingPreference(UiReadingPreferenceUpdateRequestDto req) {
    Objects.requireNonNull(req, "req");

    Usuario u = currentUser.getRequired();
    long uid = u.getId();
    UsuarioAccessibilityPreference pref = prefs.loadOrCreate(uid);

    UiReadingIntensity intensity = req.intensity() == null ? reading.getDefaultIntensity() : req.intensity();
    pref.setReadingModeEnabled(req.readingModeEnabled());
    pref.setReadingIntensity(intensity == null ? UiReadingIntensity.SOFT : intensity);
    pref.setUpdatedAt(Instant.now());

    prefs.save(pref);

    UiReadingPreferenceDto dto = new UiReadingPreferenceDto(pref.isReadingModeEnabled(), pref.getReadingIntensity(), pref.getUpdatedAt());

    appendLedger(uid, "UI_READING_PREF_UPDATE", Map.of(
        "enabled", dto.readingModeEnabled(),
        "intensity", dto.intensity().name()
    ));

    publishPresentationLive(uid, "READING_PREF");

    return dto;
  }

  @Transactional
  public void publishPresentationLive(long usuarioId, String reason) {
    UsuarioAccessibilityPreference pref = prefs.loadOrCreate(usuarioId);
    Instant now = Instant.now();
    UiPresentationDto light = compile(pref, UiTheme.LIGHT, now, usuarioId);
    UiPresentationDto dark = compile(pref, UiTheme.DARK, now, usuarioId);

    Map<String, Object> payload = Map.of(
        "type", "UI_PRESENTATION",
        "at", now.toString(),
        "usuarioId", usuarioId,
        "reason", reason == null ? "" : reason,
        "light", light,
        "dark", dark
    );

    String topic = "UIP:" + usuarioId;
    String dk = "uiPresentationLive:" + hasher.fingerprint(Map.of(
        "usuarioId", usuarioId,
        "light", light.presentationHash(),
        "dark", dark.presentationHash(),
        "reason", reason == null ? "" : reason
    )).sha256();
    outbox.enqueue(
        topic,
        OutboxPublisher.EVT_UI_PRESENTATION_LIVE,
        payload,
        Map.of("topic", topic),
        dk,
        "USUARIO",
        Long.toString(usuarioId)
    );
  }

  private UiPresentationDto compile(UsuarioAccessibilityPreference pref, UiTheme theme, Instant at, long usuarioId) {
    UiAccessibilityPreset preset = pref.getPreset() == null ? UiAccessibilityPreset.DEFAULT : pref.getPreset();
    long flagsMask = pref.getAccessibilityFlags();
    List<UiAccessibilityFlag> flags = List.copyOf(UiAccessibilityFlag.fromMask(flagsMask));
    boolean readingEnabled = pref.isReadingModeEnabled();
    UiReadingIntensity intensity = pref.getReadingIntensity() == null ? reading.getDefaultIntensity() : pref.getReadingIntensity();

    UiPresentationCompiler.Result r = compiler.compile(theme, preset, flagsMask, readingEnabled, intensity);

    
    String wm = buildWatermarkText(usuarioId);
    java.util.Map<String, String> tokenMap = new java.util.LinkedHashMap<>(r.tokenMap());
    if (wm != null && !wm.isBlank()) {
      tokenMap.put(UiCssTokenKey.WATERMARK_TEXT.css(), wm);
    }

    
    java.util.List<com.tcc.pjb.backend.model.dto.ui.presentation.UiCssTokenDto> tokens = new java.util.ArrayList<>(r.tokens());
    if (wm != null && !wm.isBlank()) {
      tokens = overrideToken(tokens, UiCssTokenKey.WATERMARK_TEXT.css(), wm);
    }

    
    tokens = overrideToken(tokens, UiCssTokenKey.CHAT_ATTACH_ENABLED.css(), atendimentoAttachmentsEnabled ? "1" : "0");
    tokens = overrideToken(tokens, UiCssTokenKey.CHAT_ATTACH_MAX_BYTES.css(), Long.toString(atendimentoAttachmentMaxBytes));
    tokens = overrideToken(tokens, UiCssTokenKey.CHAT_ATTACH_MAX_PER_MESSAGE.css(), Integer.toString(atendimentoAttachmentMaxPerMessage));

    String hash = hasher.fingerprint(Map.of(
        "theme", theme.name(),
        "variant", r.variant().name(),
        "preset", preset.name(),
        "flagsMask", flagsMask,
        "readingEnabled", readingEnabled,
        "intensity", intensity.name(),
        "tokens", tokenMap
    )).sha256();

    return new UiPresentationDto(
        theme,
        r.variant(),
        preset,
        flagsMask,
        flags,
        readingEnabled,
        intensity,
        List.copyOf(tokens),
        hash,
        at
    );
  }

  private String buildWatermarkText(long usuarioId) {
    if (usuarioId <= 0) return null;
    String base = "USUARIO " + usuarioId;
    try {
      Usuario u = usuarioRepo.findById(usuarioId).orElse(null);
      String cpf = u == null ? null : u.getCpf();
      String digits = cpf == null ? null : cpf.replaceAll("\\D", "");
      if (digits != null && digits.length() >= 4) {
        String last4 = digits.substring(digits.length() - 4);
        base = base + " • CPF ***" + last4;
      }
    } catch (Exception ignored) {
    }
    if (base.length() > 48) {
      base = base.substring(0, 48);
    }
    return base;
  }

  private static java.util.List<com.tcc.pjb.backend.model.dto.ui.presentation.UiCssTokenDto> overrideToken(
      java.util.List<com.tcc.pjb.backend.model.dto.ui.presentation.UiCssTokenDto> tokens,
      String key,
      String value
  ) {
    if (tokens == null || tokens.isEmpty() || key == null || key.isBlank() || value == null || value.isBlank()) {
      return tokens == null ? java.util.List.of() : tokens;
    }
    java.util.List<com.tcc.pjb.backend.model.dto.ui.presentation.UiCssTokenDto> out = new java.util.ArrayList<>(tokens.size());
    boolean replaced = false;
    for (com.tcc.pjb.backend.model.dto.ui.presentation.UiCssTokenDto t : tokens) {
      if (!replaced && key.equals(t.key())) {
        out.add(new com.tcc.pjb.backend.model.dto.ui.presentation.UiCssTokenDto(key, value));
        replaced = true;
      } else {
        out.add(t);
      }
    }
    if (!replaced) {
      out.add(new com.tcc.pjb.backend.model.dto.ui.presentation.UiCssTokenDto(key, value));
    }
    return java.util.List.copyOf(out);
  }

  private void appendLedger(long uid, String action, Map<String, Object> payload) {
    try {
      String json = mapper.writeValueAsString(payload);
      String h = hasher.fingerprint(payload).sha256();
      audit.append(action, "USUARIO", Long.toString(uid), h, json);
    } catch (Exception ignored) {
    }
  }
}
