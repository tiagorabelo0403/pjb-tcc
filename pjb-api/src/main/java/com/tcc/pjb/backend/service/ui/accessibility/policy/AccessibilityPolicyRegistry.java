package com.tcc.pjb.backend.service.ui.accessibility.policy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public final class AccessibilityPolicyRegistry {

  private static final Logger log = LoggerFactory.getLogger(AccessibilityPolicyRegistry.class);

  private final ObjectMapper mapper;
  private final Environment env;

  private final AtomicReference<State> state = new AtomicReference<>();
  private final AtomicLong version = new AtomicLong(1);

  private volatile Path externalFile;
  private volatile long lastModified = -1L;

  public AccessibilityPolicyRegistry(ObjectMapper mapper, Environment env) {
    this.mapper = Objects.requireNonNull(mapper, "mapper");
    this.env = Objects.requireNonNull(env, "env");
    bootstrap();
  }

  public long version() {
    return version.get();
  }

  public AccessibilityPolicyFile policy() {
    return state.get().policy;
  }

  public Instant loadedAt() {
    return state.get().loadedAt;
  }

  public double weight(String signalCode) {
    if (signalCode == null || signalCode.isBlank()) {
      return 0.0;
    }
    Double w = state.get().signalWeights.get(signalCode.trim().toUpperCase());
    return w == null ? 0.0 : w;
  }

  public String reasonText(String signalCode) {
    if (signalCode == null || signalCode.isBlank()) {
      return "";
    }
    String k = signalCode.trim().toUpperCase();
    String v = state.get().reasonCatalog.get(k);
    return v == null ? k : v;
  }

  public List<String> presetPriority() {
    return state.get().presetPriority;
  }

  @org.springframework.scheduling.annotation.Scheduled(fixedDelayString = "${pjb.ui.accessibility.policyReloadMs:5000}")
  public void reloadIfNeeded() {
    Path f = externalFile;
    if (f == null) return;
    try {
      long lm = Files.getLastModifiedTime(f).toMillis();
      if (lm <= lastModified) return;
      AccessibilityPolicyFile p = readFromFile(f);
      apply(p);
      lastModified = lm;
      log.info("Accessibility policy reloaded from {}", f);
    } catch (Exception ex) {
      log.warn("Accessibility policy reload failed (keeping previous): {}", ex.getMessage());
    }
  }

  private void bootstrap() {
    AccessibilityPolicyFile base = readFromClasspath();
    apply(base);

    String file = env.getProperty("pjb.ui.accessibility.policyFile");
    if (file != null && !file.isBlank()) {
      try {
        Path p = Path.of(file.trim());
        if (Files.exists(p) && Files.isRegularFile(p)) {
          this.externalFile = p;
          this.lastModified = Files.getLastModifiedTime(p).toMillis();
          AccessibilityPolicyFile override = readFromFile(p);
          apply(override);
          log.info("Accessibility policy override loaded from {}", p);
        } else {
          log.warn("pjb.ui.accessibility.policyFile set but not found: {}", p);
        }
      } catch (Exception ex) {
        log.warn("Failed to load pjb.ui.accessibility.policyFile: {}", ex.getMessage());
      }
    }
  }

  private AccessibilityPolicyFile readFromClasspath() {
    try {
      ClassPathResource r = new ClassPathResource("ui/accessibility-policy.json");
      return mapper.readValue(r.getInputStream(), AccessibilityPolicyFile.class);
    } catch (Exception ex) {
      log.warn("Default ui/accessibility-policy.json missing/invalid, using hard fallback: {}", ex.getMessage());
      return new AccessibilityPolicyFile(
          1,
          new AccessibilityPolicyFile.Model(-1.25, 1.0, 320, 6),
          List.of("HIGH_CONTRAST", "SCREEN_READER_OPTIMIZED", "LARGE_TEXT", "REDUCED_MOTION", "KEYBOARD_ONLY", "DEFAULT"),
          Map.of(),
          Map.of()
      );
    }
  }

  private AccessibilityPolicyFile readFromFile(Path p) throws IOException {
    byte[] bytes = Files.readAllBytes(p);
    return mapper.readValue(bytes, AccessibilityPolicyFile.class);
  }

  private void apply(AccessibilityPolicyFile p) {
    if (p == null) return;
    List<String> prio = p.presetPriority() == null || p.presetPriority().isEmpty()
        ? List.of("HIGH_CONTRAST", "SCREEN_READER_OPTIMIZED", "LARGE_TEXT", "REDUCED_MOTION", "KEYBOARD_ONLY", "DEFAULT")
        : List.copyOf(p.presetPriority());
    Map<String, Double> weights = p.signalWeights() == null ? Map.of() : Map.copyOf(p.signalWeights());
    Map<String, String> reasons = p.reasonCatalog() == null ? Map.of() : Map.copyOf(p.reasonCatalog());

    state.set(new State(p, prio, weights, reasons, Instant.now()));
    version.incrementAndGet();
  }

  private record State(
      AccessibilityPolicyFile policy,
      List<String> presetPriority,
      Map<String, Double> signalWeights,
      Map<String, String> reasonCatalog,
      Instant loadedAt
  ) {
  }
}
