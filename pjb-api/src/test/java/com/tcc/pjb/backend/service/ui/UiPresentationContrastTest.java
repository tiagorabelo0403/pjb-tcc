package com.tcc.pjb.backend.service.ui;

import com.tcc.pjb.backend.model.dto.ui.UiTheme;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityFlag;
import com.tcc.pjb.backend.model.dto.ui.accessibility.UiAccessibilityPreset;
import com.tcc.pjb.backend.model.dto.ui.presentation.UiReadingIntensity;
import com.tcc.pjb.backend.service.ui.presentation.ReadingModeProperties;
import com.tcc.pjb.backend.service.ui.presentation.color.UiColorMath;
import com.tcc.pjb.backend.service.ui.presentation.compiler.UiCssTokenKey;
import com.tcc.pjb.backend.service.ui.presentation.compiler.UiPresentationCompiler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UiPresentationContrastTest {

  @Test
  void enforcesContrastAcrossVariants() {
    ReadingModeProperties props = new ReadingModeProperties();
    props.setEnabledByDefault(true);
    UiPresentationCompiler c = new UiPresentationCompiler(props);

    for (UiTheme theme : UiTheme.values()) {
      for (UiReadingIntensity intensity : UiReadingIntensity.values()) {
        assertMin(c, theme, intensity, 0L, 4.5, 3.5, 4.5);
        assertMin(c, theme, intensity, UiAccessibilityFlag.HIGH_CONTRAST.bit(), 7.0, 7.0, 7.0);
      }
    }
  }

  private static void assertMin(UiPresentationCompiler c, UiTheme theme, UiReadingIntensity intensity, long flags, double minText, double minMuted, double minLink) {
    var r = c.compile(theme, UiAccessibilityPreset.DEFAULT, flags, true, intensity);
    String bg = r.tokenMap().get(UiCssTokenKey.BG.css());
    String text = r.tokenMap().get(UiCssTokenKey.TEXT.css());
    String muted = r.tokenMap().get(UiCssTokenKey.MUTED.css());
    String link = r.tokenMap().get(UiCssTokenKey.LINK.css());

    Assertions.assertTrue(UiColorMath.contrastRatio(text, bg) >= minText);
    Assertions.assertTrue(UiColorMath.contrastRatio(muted, bg) >= minMuted);
    Assertions.assertTrue(UiColorMath.contrastRatio(link, bg) >= minLink);
  }
}
