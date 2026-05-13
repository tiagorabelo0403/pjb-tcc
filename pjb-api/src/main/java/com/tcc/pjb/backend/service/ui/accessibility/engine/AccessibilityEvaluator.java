package com.tcc.pjb.backend.service.ui.accessibility.engine;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityFlag;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityPreset;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiUsageMetricsDto;
import com.tcc.pjb.backend.platform.hash.CanonicalJsonHasher;
import com.tcc.pjb.backend.service.ui.accessibility.policy.AccessibilityPolicyFile;
import com.tcc.pjb.backend.service.ui.accessibility.policy.AccessibilityPolicyRegistry;

@Component
public final class AccessibilityEvaluator {

  private final AccessibilityPolicyRegistry policy;
  private final CanonicalJsonHasher hasher;

  public AccessibilityEvaluator(AccessibilityPolicyRegistry policy, CanonicalJsonHasher hasher) {
    this.policy = Objects.requireNonNull(policy, "policy");
    this.hasher = Objects.requireNonNull(hasher, "hasher");
  }

  public AccessibilityEvaluation evaluate(UiUsageMetricsDto m) {
    Objects.requireNonNull(m, "metrics");

    AccessibilityPolicyFile p = policy.policy();
    AccessibilityPolicyFile.Model model = p.model();

    List<String> codes = new ArrayList<>(10);
    List<String> reasons = new ArrayList<>(10);

    double z = model.bias();

    if (isTrue(m.forcedColors())) {
      z += policy.weight(AccessibilitySignalCode.FORCED_COLORS.name());
      codes.add(AccessibilitySignalCode.FORCED_COLORS.name());
      reasons.add(policy.reasonText(AccessibilitySignalCode.FORCED_COLORS.name()));
    }
    if (isTrue(m.prefersReducedMotion())) {
      z += policy.weight(AccessibilitySignalCode.PREFERS_REDUCED_MOTION.name());
      codes.add(AccessibilitySignalCode.PREFERS_REDUCED_MOTION.name());
      reasons.add(policy.reasonText(AccessibilitySignalCode.PREFERS_REDUCED_MOTION.name()));
    }
    if (isTrue(m.screenReaderHint())) {
      z += policy.weight(AccessibilitySignalCode.SCREEN_READER_HINT.name());
      codes.add(AccessibilitySignalCode.SCREEN_READER_HINT.name());
      reasons.add(policy.reasonText(AccessibilitySignalCode.SCREEN_READER_HINT.name()));
    }

    int zoom = clamp(m.zoomEventsLast30d(), 0, 999);
    if (zoom >= 6) {
      z += policy.weight(AccessibilitySignalCode.ZOOM_FREQUENT.name());
      codes.add(AccessibilitySignalCode.ZOOM_FREQUENT.name());
      reasons.add(policy.reasonText(AccessibilitySignalCode.ZOOM_FREQUENT.name()));
    }

    int hc = clamp(m.highContrastTogglesLast30d(), 0, 999);
    if (hc >= 2) {
      z += policy.weight(AccessibilitySignalCode.HIGH_CONTRAST_TOGGLES.name());
      codes.add(AccessibilitySignalCode.HIGH_CONTRAST_TOGGLES.name());
      reasons.add(policy.reasonText(AccessibilitySignalCode.HIGH_CONTRAST_TOGGLES.name()));
    }

    int font = clamp(m.fontScalePercent(), 50, 300);
    if (font >= 125) {
      z += policy.weight(AccessibilitySignalCode.FONT_SCALE_HIGH.name());
      codes.add(AccessibilitySignalCode.FONT_SCALE_HIGH.name());
      reasons.add(policy.reasonText(AccessibilitySignalCode.FONT_SCALE_HIGH.name()));
    }

    int kbd = clamp(m.keyboardNavigationRate(), 0, 100);
    int mouse = clamp(m.mouseNavigationRate(), 0, 100);
    if (kbd >= 75 && mouse <= 25) {
      z += policy.weight(AccessibilitySignalCode.KEYBOARD_DOMINANT.name());
      codes.add(AccessibilitySignalCode.KEYBOARD_DOMINANT.name());
      reasons.add(policy.reasonText(AccessibilitySignalCode.KEYBOARD_DOMINANT.name()));
    }

    double prob = sigmoid(model.k() * z);
    int score = (int) Math.round(prob * 1000.0);
    double confidence = computeConfidence(codes.size(), m);

    EnumSet<UiAccessibilityFlag> flags = deriveFlags(codes, font);
    UiAccessibilityPreset legacyPreset = deriveLegacyPreset(flags);
    long flagsMask = UiAccessibilityFlag.maskOf(flags);

    String suggestionHash = hasher.fingerprint(new FingerprintPayload(
        p.version(),
        flagsMask,
        score,
        codes,
        safeMetricsFingerprint(m)
    )).sha256();

    int max = model.maxReasons();
    List<String> outCodes = codes.size() <= max ? List.copyOf(codes) : List.copyOf(codes.subList(0, max));
    List<String> outReasons = reasons.size() <= max ? List.copyOf(reasons) : List.copyOf(reasons.subList(0, max));

    List<UiAccessibilityFlag> outFlags = List.copyOf(flags);

    return new AccessibilityEvaluation(
        legacyPreset,
        flagsMask,
        outFlags,
        score,
        prob,
        confidence,
        outCodes,
        outReasons,
        suggestionHash
    );
  }

  private EnumSet<UiAccessibilityFlag> deriveFlags(List<String> codes, int fontScale) {
    EnumSet<UiAccessibilityFlag> set = EnumSet.noneOf(UiAccessibilityFlag.class);

    if (codes.contains(AccessibilitySignalCode.FORCED_COLORS.name()) || codes.contains(AccessibilitySignalCode.HIGH_CONTRAST_TOGGLES.name())) {
      set.add(UiAccessibilityFlag.HIGH_CONTRAST);
    }
    if (codes.contains(AccessibilitySignalCode.SCREEN_READER_HINT.name())) {
      set.add(UiAccessibilityFlag.SCREEN_READER_OPTIMIZED);
    }
    if (fontScale >= 125 || codes.contains(AccessibilitySignalCode.FONT_SCALE_HIGH.name()) || codes.contains(AccessibilitySignalCode.ZOOM_FREQUENT.name())) {
      set.add(UiAccessibilityFlag.LARGE_TEXT);
    }
    if (codes.contains(AccessibilitySignalCode.PREFERS_REDUCED_MOTION.name())) {
      set.add(UiAccessibilityFlag.REDUCED_MOTION);
    }
    if (codes.contains(AccessibilitySignalCode.KEYBOARD_DOMINANT.name())) {
      set.add(UiAccessibilityFlag.KEYBOARD_ONLY);
    }

    return set;
  }

  private UiAccessibilityPreset deriveLegacyPreset(EnumSet<UiAccessibilityFlag> flags) {
    if (flags.contains(UiAccessibilityFlag.HIGH_CONTRAST)) return UiAccessibilityPreset.HIGH_CONTRAST;
    if (flags.contains(UiAccessibilityFlag.SCREEN_READER_OPTIMIZED)) return UiAccessibilityPreset.SCREEN_READER_OPTIMIZED;
    if (flags.contains(UiAccessibilityFlag.LARGE_TEXT)) return UiAccessibilityPreset.LARGE_TEXT;
    if (flags.contains(UiAccessibilityFlag.REDUCED_MOTION)) return UiAccessibilityPreset.REDUCED_MOTION;
    if (flags.contains(UiAccessibilityFlag.KEYBOARD_ONLY)) return UiAccessibilityPreset.KEYBOARD_ONLY;
    return UiAccessibilityPreset.DEFAULT;
  }

  private static boolean isTrue(Boolean v) {
    return v != null && v;
  }

  private static int clamp(Integer v, int min, int max) {
    if (v == null) return min;
    return Math.max(min, Math.min(max, v));
  }

  private static double sigmoid(double x) {
    if (x >= 0) {
      double e = Math.exp(-x);
      return 1.0 / (1.0 + e);
    }
    double e = Math.exp(x);
    return e / (1.0 + e);
  }

  private static double computeConfidence(int signalCount, UiUsageMetricsDto m) {
    double base = 0.55 + Math.min(0.35, signalCount * 0.07);
    int k = clamp(m.keyboardNavigationRate(), 0, 100);
    int ms = clamp(m.mouseNavigationRate(), 0, 100);
    if (k >= 70 && ms >= 70) {
      base -= 0.15;
    }
    return Math.max(0.10, Math.min(0.95, base));
  }

  private static String safeMetricsFingerprint(UiUsageMetricsDto m) {
    String h = m.headersEvidence() == null ? "" : m.headersEvidence().toString();
    return (
        "prm=" + m.prefersReducedMotion()
            + ",fc=" + m.forcedColors()
            + ",sr=" + m.screenReaderHint()
            + ",zoom=" + m.zoomEventsLast30d()
            + ",hc=" + m.highContrastTogglesLast30d()
            + ",kbd=" + m.keyboardNavigationRate()
            + ",mouse=" + m.mouseNavigationRate()
            + ",font=" + m.fontScalePercent()
            + ",hdr=" + h
    ).toUpperCase(Locale.ROOT);
  }

  private record FingerprintPayload(
      int policyVersion,
      long flagsMask,
      int score,
      List<String> reasonCodes,
      String metrics
  ) {
  }
}
