package com.tcc.pjb.backend.service.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityFlag;
import com.tcc.pjb.backend.service.ui.accessibility.governance.AccessibilityAbacPolicyFile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

public class UiAccessibilityAbacPolicyTest {

  @Test
  void abacMasksAreSane() throws Exception {
    long all = 0L;
    for (UiAccessibilityFlag f : UiAccessibilityFlag.values()) {
      all |= f.bit();
    }

    ObjectMapper om = new ObjectMapper();
    try (InputStream in = getClass().getClassLoader().getResourceAsStream("ui/accessibility-abac.json")) {
      Assertions.assertNotNull(in);
      AccessibilityAbacPolicyFile f = om.readValue(in, AccessibilityAbacPolicyFile.class);
      Assertions.assertNotNull(f.defaultDecision());

      checkDecision(all, f.defaultDecision());
      if (f.rules() != null) {
        for (AccessibilityAbacPolicyFile.Rule r : f.rules()) {
          if (r.decision() != null) {
            checkDecision(all, r.decision());
          }
        }
      }
    }
  }

  private static void checkDecision(long all, AccessibilityAbacPolicyFile.Decision d) {
    long allow = d.allowFlagsMask();
    long deny = d.denyFlagsMask();
    Assertions.assertEquals(0L, (allow & deny));
    Assertions.assertEquals(0L, (allow & ~all));
    Assertions.assertEquals(0L, (deny & ~all));
  }
}
