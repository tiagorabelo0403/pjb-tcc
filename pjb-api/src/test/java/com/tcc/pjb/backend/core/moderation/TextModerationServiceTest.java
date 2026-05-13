package com.tcc.pjb.backend.core.moderation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TextModerationServiceTest {

  @Test
  void allowsCleanText() {
    TextModerationService s = new TextModerationService();
    String out = s.validateMessage("Boa tarde, doutor. A ultima movimentacao foi juntada de documentos.");
    Assertions.assertTrue(out.startsWith("Boa tarde"));
  }

  @Test
  void blocksOffensiveWord() {
    TextModerationService s = new TextModerationService();
    Assertions.assertThrows(ContentBlockedException.class, () -> s.validateMessage("isso e uma merda"));
  }

  @Test
  void blocksEmbeddedImage() {
    TextModerationService s = new TextModerationService();
    Assertions.assertThrows(ContentBlockedException.class, () -> s.validateMessage("data:image/png;base64,AAAA"));
  }

  @Test
  void blocksMediaLink() {
    TextModerationService s = new TextModerationService();
    Assertions.assertThrows(ContentBlockedException.class, () -> s.validateMessage("veja https://exemplo.com/foto.jpg"));
  }
}
