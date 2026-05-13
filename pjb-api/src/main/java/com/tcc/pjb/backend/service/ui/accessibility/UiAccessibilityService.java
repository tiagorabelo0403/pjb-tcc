package com.tcc.pjb.backend.service.ui.accessibility;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.orchestrator.IAOrchestrator;
import com.tcc.pjb.backend.ai.skills.v1.SkillUiAccessibilitySuggestV1;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.governance.idempotency.RequestIdempotencyBeginResult;
import com.tcc.pjb.backend.core.governance.idempotency.IdempotencyInProgressException;
import com.tcc.pjb.backend.core.governance.idempotency.RequestIdempotencyService;
import com.tcc.pjb.backend.core.resilience.LocalCircuitBreaker;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityFlag;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityLoginContextDto;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityPreferenceDto;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityPreferenceUpdateRequestDto;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityPreset;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilitySuggestionDto;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiUsageMetricsDto;
import com.tcc.pjb.backend.model.dto.ui.presentation.UiPresentationBundleDto;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.ui.AccessibilityUsageSnapshot;
import com.tcc.pjb.backend.model.entity.ui.UsuarioAccessibilityPreference;
import com.tcc.pjb.backend.platform.hash.CanonicalJsonHasher;
import com.tcc.pjb.backend.repository.ui.AccessibilityUsageSnapshotRepository;
import com.tcc.pjb.backend.repository.ui.UsuarioAccessibilityPreferenceRepository;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.service.ui.accessibility.engine.AccessibilityEvaluation;
import com.tcc.pjb.backend.service.ui.accessibility.engine.AccessibilityEvaluator;
import com.tcc.pjb.backend.service.ui.accessibility.governance.AccessibilityAbacRegistry;
import com.tcc.pjb.backend.service.ui.accessibility.policy.AccessibilityPolicyRegistry;
import com.tcc.pjb.backend.service.ui.accessibility.security.AccessibilitySuggestionTokenService;
import com.tcc.pjb.backend.service.ui.governance.UiPolicyIntegrityState;
import com.tcc.pjb.backend.service.ui.preferences.UiUserPreferenceService;
import com.tcc.pjb.backend.service.ui.presentation.UiPresentationService;

@Service
public class UiAccessibilityService {

  private static final Logger log = LoggerFactory.getLogger(UiAccessibilityService.class);

  private final AccessibilityProperties props;
  private final AccessibilityPolicyRegistry policy;
  private final CanonicalJsonHasher hasher;
  private final ObjectMapper mapper;

  private final CurrentUserService currentUser;
  private final UsuarioAccessibilityPreferenceRepository prefRepo;
  private final UiUserPreferenceService userPrefs;
  private final AccessibilityUsageSnapshotRepository snapshotRepo;

  private final AccessibilitySuggestionTokenService tokenService;
  private final RequestIdempotencyService idempotency;
  private final AuditLedgerService audit;
  private final OutboxPublisher outbox;

  private final UiPresentationService presentation;

  private final AccessibilityAbacRegistry abac;

  private final IAOrchestrator ia;

  private final AccessibilityEvaluator evaluator;

  private final UiPolicyIntegrityState integrity;
  private final LocalCircuitBreaker orchestratorBreaker;

  public UiAccessibilityService(
      AccessibilityProperties props,
      AccessibilityPolicyRegistry policy,
      CanonicalJsonHasher hasher,
      ObjectMapper mapper,
      CurrentUserService currentUser,
      UsuarioAccessibilityPreferenceRepository prefRepo,
      UiUserPreferenceService userPrefs,
      AccessibilityUsageSnapshotRepository snapshotRepo,
      AccessibilitySuggestionTokenService tokenService,
      RequestIdempotencyService idempotency,
      AuditLedgerService audit,
      OutboxPublisher outbox,
      UiPresentationService presentation,
      AccessibilityAbacRegistry abac,
      IAOrchestrator ia,
      AccessibilityEvaluator evaluator,
      UiPolicyIntegrityState integrity
  ) {
    this.props = Objects.requireNonNull(props, "props");
    this.policy = Objects.requireNonNull(policy, "policy");
    this.hasher = Objects.requireNonNull(hasher, "hasher");
    this.mapper = Objects.requireNonNull(mapper, "mapper");
    this.currentUser = Objects.requireNonNull(currentUser, "currentUser");
    this.prefRepo = Objects.requireNonNull(prefRepo, "prefRepo");
    this.userPrefs = Objects.requireNonNull(userPrefs, "userPrefs");
    this.snapshotRepo = Objects.requireNonNull(snapshotRepo, "snapshotRepo");
    this.tokenService = Objects.requireNonNull(tokenService, "tokenService");
    this.idempotency = Objects.requireNonNull(idempotency, "idempotency");
    this.audit = Objects.requireNonNull(audit, "audit");
    this.outbox = Objects.requireNonNull(outbox, "outbox");
    this.presentation = Objects.requireNonNull(presentation, "presentation");
    this.abac = Objects.requireNonNull(abac, "abac");
    this.ia = Objects.requireNonNull(ia, "ia");
    this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
    this.integrity = Objects.requireNonNull(integrity, "integrity");
    this.orchestratorBreaker = new LocalCircuitBreaker(Clock.systemUTC(), 3, 60_000L);
  }

  @Transactional
  public UiAccessibilityLoginContextDto evaluateOnLogin(UiUsageMetricsDto metrics) {
    Usuario u = currentUser.getRequired();
    long uid = u.getId();
    Instant now = Instant.now();

    UsuarioAccessibilityPreference pref = userPrefs.loadOrCreate(uid);
    long currentMask = pref.getAccessibilityFlags();
    List<UiAccessibilityFlag> currentFlags = List.copyOf(UiAccessibilityFlag.fromMask(currentMask));

    
    String requestHash = hasher.fingerprint(new LoginContextFingerprint(uid, policy.policy().version(), metrics)).sha256();
    try {
      RequestIdempotencyBeginResult begin = idempotency.begin("UI_A11Y_LOGIN_CONTEXT", requestHash, Duration.ofSeconds(45));
      if (begin.isCompleted() && begin.responseJson() != null && !begin.responseJson().isBlank()) {
        return mapper.readValue(begin.responseJson(), UiAccessibilityLoginContextDto.class);
      }
    } catch (IdempotencyInProgressException inProgress) {
      
      UiPresentationBundleDto bundle = presentation.bundleForUserId(uid);
      return new UiAccessibilityLoginContextDto(
          pref.getPreset(),
          currentMask,
          currentFlags,
          pref.isSuppressSuggestions(),
          pref.getNextEligibleAt(),
          null,
          bundle,
          now
      );
    } catch (Exception e) {
      log.debug("Falha ao carregar bundle de apresentação para contexto de acessibilidade: {}", e.getMessage());
    }

    UiAccessibilitySuggestionDto suggestion = null;
    AccessibilityAbacRegistry.AccessibilitySubject subject = AccessibilityAbacRegistry.AccessibilitySubject.of(
        u.getUf(),
        u.getComarca(),
        u.getTipoUsuario() == null ? null : u.getTipoUsuario().name(),
        u.getEnteFederativo() == null ? null : u.getEnteFederativo().name()
    );
    var decision = abac.decide(subject);
    boolean maySuggest = !integrity.isDegraded() && props.isEnabled() && decision.enabled() && pref.canSuggestNow(now) && eligibleToReevaluate(pref, now);

    if (maySuggest) {
      AccessibilityEvaluation eval = props.isUseOrchestrator()
          ? evaluateViaOrchestrator(uid, metrics)
          : evaluator.evaluate(metrics);

      long allowed = decision.allowFlagsMask();
      long denied = decision.denyFlagsMask();
      long eff = eval.flagsMask();
      if (allowed != 0L) {
        eff &= allowed;
      }
      if (denied != 0L) {
        eff &= ~denied;
      }

      UiAccessibilityPreset legacyPreset = eval.legacyPreset();
      List<UiAccessibilityFlag> effFlags = List.copyOf(UiAccessibilityFlag.fromMask(eff));
      if (eff == 0L) {
        effFlags = List.of();
        legacyPreset = UiAccessibilityPreset.DEFAULT;
      }

      pref.setLastEvaluatedAt(now);
      pref.setLastSuggestionHash(eval.suggestionHash());
      pref.setUpdatedAt(now);

      int threshold = Math.max(Math.max(props.getMinScoreToSuggest(), policy.policy().model().minScoreToSuggest()), decision.minScoreToSuggest());
      if (eval.score() >= threshold && eff != 0L) {
        String token = tokenService.mint(uid, eval.suggestionHash(), Duration.ofMinutes(15));
        suggestion = new UiAccessibilitySuggestionDto(
            legacyPreset,
            eff,
            effFlags,
            eval.score(),
            eval.probability(),
            eval.confidence(),
            eval.reasonCodes(),
            eval.reasons(),
            eval.suggestionHash(),
            token,
            now
        );

        
        persistSnapshot(uid, now, eval, metrics);

        
        appendLedger("UI_A11Y_SUGGEST_SHOWN", "USUARIO", Long.toString(uid), Map.of(
            "score", eval.score(),
            "preset", legacyPreset.name(),
            "flagsMask", eff,
            "hash", eval.suggestionHash(),
            "policyVersion", policy.policy().version(),
            "abacVersion", abac.policy().version()
        ));
      }
    }

    prefRepo.save(pref);
    UiPresentationBundleDto bundle = presentation.bundleForUserId(uid);
    UiAccessibilityLoginContextDto out = new UiAccessibilityLoginContextDto(
        pref.getPreset(),
        currentMask,
        currentFlags,
        pref.isSuppressSuggestions(),
        pref.getNextEligibleAt(),
        suggestion,
        bundle,
        now
    );

    try {
      String responseJson = mapper.writeValueAsString(out);
      String responseHash = hasher.fingerprint(out).sha256();
      idempotency.complete(requestHash, "USUARIO", Long.toString(uid), responseHash, responseJson);
    } catch (Exception ignore) {
      
    }

    return out;
  }

  private AccessibilityEvaluation evaluateViaOrchestrator(long uid, UiUsageMetricsDto metrics) {
    if (!orchestratorBreaker.tryAcquire()) {
      return evaluator.evaluate(metrics);
    }
    try {
      IARequest req = IARequest.builder()
          .withOrigem("UI_ACCESSIBILITY")
          .withAcao(SkillUiAccessibilitySuggestV1.ACTION)
          .withUsuarioId(Long.toString(uid))
          .withRequestId("A11Y-" + UUID.randomUUID())
          .addPayload("metrics", metrics)
          .addPayload("policyVersion", policy.policy().version())
          .build();

      IAResponse res = ia.processar(req);
      Map<String, Object> essence = res.getEssence();
      if (essence == null || essence.isEmpty()) {
        return evaluator.evaluate(metrics);
      }

      UiAccessibilityPreset preset = UiAccessibilityPreset.fromString(String.valueOf(essence.get("preset")));
      long flagsMask = asLong(essence.get("flagsMask"), presetToMask(preset));
      List<UiAccessibilityFlag> flags = List.copyOf(UiAccessibilityFlag.fromMask(flagsMask));

      int score = asInt(essence.get("score"), 0);
      double prob = asDouble(essence.get("probability"), 0.0);
      double conf = asDouble(essence.get("confidence"), 0.3);
      @SuppressWarnings("unchecked")
      List<String> reasonCodes = (essence.get("reasonCodes") instanceof List<?> l)
          ? l.stream().map(String::valueOf).toList()
          : List.of();
      @SuppressWarnings("unchecked")
      List<String> reasons = (essence.get("reasons") instanceof List<?> l)
          ? l.stream().map(String::valueOf).toList()
          : List.of();
      String hash = String.valueOf(essence.getOrDefault("suggestionHash", ""));
      if (hash == null || hash.isBlank()) {
        return evaluator.evaluate(metrics);
      }
      orchestratorBreaker.recordSuccess();
      return new AccessibilityEvaluation(preset, flagsMask, flags, score, prob, conf, reasonCodes, reasons, hash);
    } catch (Exception ex) {
      orchestratorBreaker.recordFailure();
      return evaluator.evaluate(metrics);
    }
  }

  private static long presetToMask(UiAccessibilityPreset p) {
    if (p == null) return 0L;
    return switch (p) {
      case HIGH_CONTRAST -> UiAccessibilityFlag.HIGH_CONTRAST.bit();
      case LARGE_TEXT -> UiAccessibilityFlag.LARGE_TEXT.bit();
      case REDUCED_MOTION -> UiAccessibilityFlag.REDUCED_MOTION.bit();
      case SCREEN_READER_OPTIMIZED -> UiAccessibilityFlag.SCREEN_READER_OPTIMIZED.bit();
      case KEYBOARD_ONLY -> UiAccessibilityFlag.KEYBOARD_ONLY.bit();
      default -> 0L;
    };
  }

  private static int asInt(Object v, int def) {
    if (v instanceof Number n) return n.intValue();
    if (v instanceof String s) {
      try {
        return Integer.parseInt(s.trim());
      } catch (Exception ignored) {
      }
    }
    return def;
  }

  private static double asDouble(Object v, double def) {
    if (v instanceof Number n) return n.doubleValue();
    if (v instanceof String s) {
      try {
        return Double.parseDouble(s.trim());
      } catch (Exception ignored) {
      }
    }
    return def;
  }

  private static long asLong(Object v, long def) {
    if (v instanceof Number n) return n.longValue();
    if (v instanceof String s) {
      try {
        return Long.parseLong(s.trim());
      } catch (Exception ignored) {
      }
    }
    return def;
  }

  @Transactional
  public UiAccessibilityPreferenceDto getPreference() {
    Usuario u = currentUser.getRequired();
    long uid = u.getId();
    UsuarioAccessibilityPreference pref = userPrefs.loadOrCreate(uid);
    return toDto(pref);
  }

  @Transactional
  public UiAccessibilityPreferenceDto updatePreference(UiAccessibilityPreferenceUpdateRequestDto req) {
    Objects.requireNonNull(req, "req");

    Usuario u = currentUser.getRequired();
    long uid = u.getId();
    Instant now = Instant.now();

    UsuarioAccessibilityPreference pref = userPrefs.loadOrCreate(uid);
    String decision = req.decision() == null ? "" : req.decision().trim().toUpperCase();

    
    if (decision.equals("ACCEPT") || decision.equals("DECLINE") || decision.equals("SNOOZE")) {
      String expectedHash = pref.getLastSuggestionHash();
      tokenService.verifyRequired(req.token(), uid, expectedHash);
    }

    switch (decision) {
      case "ACCEPT" -> {
        long mask = resolveFlagsMask(req);
        UiAccessibilityPreset p = resolveLegacyPreset(req, mask);
        pref.setAccessibilityFlags(mask);
        pref.setPreset(p);
        pref.setSource(UsuarioAccessibilityPreference.Source.SUGGESTION);
        pref.setAcceptedAt(now);
        pref.setNextEligibleAt(now.plus(props.getSnoozeDuration()));
      }
      case "DECLINE", "SNOOZE" -> pref.setNextEligibleAt(now.plus(props.getSnoozeDuration()));
      default -> {
        long mask = resolveFlagsMask(req);
        if (mask != pref.getAccessibilityFlags() || req.legacyPreset() != null || (req.flags() != null && !req.flags().isEmpty()) || req.flagsMask() != null) {
          UiAccessibilityPreset p = resolveLegacyPreset(req, mask);
          pref.setAccessibilityFlags(mask);
          pref.setPreset(p);
          pref.setSource(UsuarioAccessibilityPreference.Source.USER);
          pref.setAcceptedAt(now);
        }
      }
    }

    pref.setSuppressSuggestions(req.suppressSuggestions());
    pref.setUpdatedAt(now);

    UsuarioAccessibilityPreference saved = prefRepo.save(pref);

    appendLedger("UI_A11Y_PREF_UPDATE", "USUARIO", Long.toString(uid), Map.of(
        "decision", decision,
        "preset", saved.getPreset().name(),
        "flagsMask", saved.getAccessibilityFlags(),
        "suppress", saved.isSuppressSuggestions()
    ));

    publishLive(uid, saved, decision);
    try {
      presentation.publishPresentationLive(uid, "A11Y_PREF");
    } catch (Exception ignore) {
    }

    return toDto(saved);
  }

  private boolean eligibleToReevaluate(UsuarioAccessibilityPreference pref, Instant now) {
    Instant last = pref.getLastEvaluatedAt();
    if (last == null) return true;
    Duration min = props.getReevaluateMinInterval();
    return !last.plus(min == null ? Duration.ofDays(1) : min).isAfter(now);
  }

  private void persistSnapshot(long uid, Instant now, AccessibilityEvaluation eval, UiUsageMetricsDto metrics) {
    try {
      String metricsJson = mapper.writeValueAsString(metrics);
      String codes = String.join(",", eval.reasonCodes());
      String reasons = String.join(" | ", eval.reasons());
      snapshotRepo.save(new AccessibilityUsageSnapshot(
          uid,
          now,
          eval.score(),
          java.math.BigDecimal.valueOf(eval.confidence()),
          codes,
          reasons,
          metricsJson,
          policy.policy().version(),
          eval.suggestionHash()
      ));
    } catch (DataIntegrityViolationException ignore) {
      
    } catch (Exception e) {
      log.debug("snapshot save failed: {}", e.getMessage());
    }
  }

  private void publishLive(long uid, UsuarioAccessibilityPreference pref, String decision) {
    try {
      Map<String, Object> payload = Map.of(
          "type", "A11Y_PREF",
          "at", Instant.now().toString(),
          "usuarioId", uid,
          "decision", decision,
          "preset", pref.getPreset().name(),
          "flagsMask", pref.getAccessibilityFlags(),
          "suppressSuggestions", pref.isSuppressSuggestions()
      );
      String topic = "A11Y:" + uid;
      outbox.enqueue(
          topic,
          OutboxPublisher.EVT_UI_ACCESSIBILITY_LIVE,
          payload,
          Map.of("topic", topic),
          null,
          "USUARIO",
          Long.toString(uid)
      );
    } catch (Exception ignore) {
      
    }
  }

  private void appendLedger(String action, String resourceType, String resourceId, Map<String, Object> payload) {
    try {
      String payloadJson = mapper.writeValueAsString(payload == null ? Map.of() : payload);
      String payloadHash = hasher.fingerprint(payload == null ? Map.of() : payload).sha256();
      audit.append(action, resourceType, resourceId, payloadHash, payloadJson);
    } catch (Exception ignore) {
      
    }
  }

  private static UiAccessibilityPreferenceDto toDto(UsuarioAccessibilityPreference pref) {
    return new UiAccessibilityPreferenceDto(
        pref.getPreset(),
        pref.getAccessibilityFlags(),
        List.copyOf(UiAccessibilityFlag.fromMask(pref.getAccessibilityFlags())),
        pref.isSuppressSuggestions(),
        pref.getAcceptedAt(),
        pref.getUpdatedAt(),
        pref.getLastEvaluatedAt(),
        pref.getNextEligibleAt()
    );
  }

  private static long resolveFlagsMask(UiAccessibilityPreferenceUpdateRequestDto req) {
    if (req.flagsMask() != null) {
      return Math.max(0L, req.flagsMask());
    }
    if (req.flags() != null && !req.flags().isEmpty()) {
      EnumSet<UiAccessibilityFlag> set = EnumSet.noneOf(UiAccessibilityFlag.class);
      for (UiAccessibilityFlag f : req.flags()) {
        if (f != null) set.add(f);
      }
      return UiAccessibilityFlag.maskOf(set);
    }
    UiAccessibilityPreset p = req.legacyPreset() == null ? UiAccessibilityPreset.DEFAULT : req.legacyPreset();
    return switch (p) {
      case HIGH_CONTRAST -> UiAccessibilityFlag.HIGH_CONTRAST.bit();
      case LARGE_TEXT -> UiAccessibilityFlag.LARGE_TEXT.bit();
      case REDUCED_MOTION -> UiAccessibilityFlag.REDUCED_MOTION.bit();
      case SCREEN_READER_OPTIMIZED -> UiAccessibilityFlag.SCREEN_READER_OPTIMIZED.bit();
      case KEYBOARD_ONLY -> UiAccessibilityFlag.KEYBOARD_ONLY.bit();
      default -> 0L;
    };
  }

  private static UiAccessibilityPreset resolveLegacyPreset(UiAccessibilityPreferenceUpdateRequestDto req, long mask) {
    if (req.legacyPreset() != null) return req.legacyPreset();
    EnumSet<UiAccessibilityFlag> set = UiAccessibilityFlag.fromMask(mask);
    if (set.contains(UiAccessibilityFlag.HIGH_CONTRAST)) return UiAccessibilityPreset.HIGH_CONTRAST;
    if (set.contains(UiAccessibilityFlag.SCREEN_READER_OPTIMIZED)) return UiAccessibilityPreset.SCREEN_READER_OPTIMIZED;
    if (set.contains(UiAccessibilityFlag.LARGE_TEXT)) return UiAccessibilityPreset.LARGE_TEXT;
    if (set.contains(UiAccessibilityFlag.REDUCED_MOTION)) return UiAccessibilityPreset.REDUCED_MOTION;
    if (set.contains(UiAccessibilityFlag.KEYBOARD_ONLY)) return UiAccessibilityPreset.KEYBOARD_ONLY;
    return UiAccessibilityPreset.DEFAULT;
  }

  private record LoginContextFingerprint(long usuarioId, int policyVersion, UiUsageMetricsDto metrics) {
  }
}
