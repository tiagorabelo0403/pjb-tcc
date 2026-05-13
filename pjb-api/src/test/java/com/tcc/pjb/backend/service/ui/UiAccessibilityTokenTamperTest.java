package com.tcc.pjb.backend.service.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.crypto.KeyMaterialService;
import com.tcc.pjb.backend.service.ui.accessibility.security.AccessibilitySuggestionTokenService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class UiAccessibilityTokenTamperTest {

  @Test
  void tokenRejectsTamper() {
    byte[] master = new byte[32];
    for (int i = 0; i < master.length; i++) master[i] = (byte) (i + 1);
    String b64 = Base64.getEncoder().encodeToString(master);

    KeyMaterialService keys = new KeyMaterialService(b64);
    AccessibilitySuggestionTokenService svc = new AccessibilitySuggestionTokenService(keys, new ObjectMapper());

    String token = svc.mint(10L, "abc", java.time.Duration.ofMinutes(5));

    String[] parts = token.split("\\.");
    String payloadJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
    String tampered = payloadJson.replace("\"abc\"", "\"xyz\"");
    String tamperedToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tampered.getBytes(StandardCharsets.UTF_8)) + "." + parts[1];

    var v = svc.verify(tamperedToken);
    Assertions.assertFalse(v.valid());
  }
}
