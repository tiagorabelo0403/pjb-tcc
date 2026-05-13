package com.tcc.pjb.backend.service.ui.presentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.dto.ui.UiTheme;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityPreset;
import com.tcc.pjb.backend.model.dto.ui.presentation.UiReadingIntensity;
import com.tcc.pjb.backend.platform.hash.CanonicalJsonHasher;
import com.tcc.pjb.backend.service.ui.governance.UiPolicyIntegrityProperties;
import com.tcc.pjb.backend.service.ui.governance.UiPolicyIntegrityState;
import com.tcc.pjb.backend.service.ui.presentation.compiler.UiPresentationCompiler;

import java.util.Iterator;

@Configuration
public class UiPresentationBaselineEnforcer {

  private static final Logger log = LoggerFactory.getLogger(UiPresentationBaselineEnforcer.class);

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE + 10)
  public ApplicationRunner enforcePresentationBaseline(
      UiPolicyIntegrityProperties props,
      UiPolicyIntegrityState state,
      ResourceLoader loader,
      ObjectMapper mapper,
      CanonicalJsonHasher hasher,
      ReadingModeProperties reading
  ) {
    return args -> {
      if (!props.isEnforcePresentationBaseline()) return;
      if (state.isDegraded()) return;

      Resource r = loader.getResource(props.getPresentationBaselineLocation());
      if (!r.exists()) {
        state.degrade();
        return;
      }

      UiPresentationCompiler compiler = new UiPresentationCompiler(reading);

      try {
        JsonNode root = mapper.readTree(r.getInputStream());
        JsonNode hashes = root == null ? null : root.get("hashes");
        if (hashes == null || !hashes.isObject()) {
          state.degrade();
          return;
        }
        Iterator<String> it = hashes.fieldNames();
        while (it.hasNext()) {
          String key = it.next();
          JsonNode expectedNode = hashes.get(key);
          String expected = expectedNode == null ? null : expectedNode.asText();
          if (expected == null || expected.isBlank()) {
            state.degrade();
            return;
          }

          Case c = Case.parse(key);
          if (c == null) {
            state.degrade();
            return;
          }

          var res = compiler.compile(c.theme, UiAccessibilityPreset.DEFAULT, c.flagsMask, true, c.intensity);
          String actual = hasher.fingerprint(res.tokenMap()).sha256();
          if (!expected.equalsIgnoreCase(actual)) {
            log.error("presentation baseline mismatch: {}", key);
            state.degrade();
            return;
          }
        }
      } catch (Exception ex) {
        state.degrade();
      }
    };
  }

  private record Case(UiTheme theme, UiReadingIntensity intensity, long flagsMask) {

    static Case parse(String key) {
      if (key == null) return null;
      String[] parts = key.split("\\|");
      if (parts.length != 3) return null;
      UiTheme theme = UiTheme.valueOf(parts[0]);
      UiReadingIntensity intensity = UiReadingIntensity.valueOf(parts[1]);
      long flagsMask = Long.parseLong(parts[2]);
      return new Case(theme, intensity, flagsMask);
    }
  }
}
