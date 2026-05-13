package com.tcc.pjb.backend.service.ui.accessibility.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.crypto.KeyMaterialService;

@Service
public class AccessibilitySuggestionTokenService {

  private static final Base64.Encoder B64U = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder B64UD = Base64.getUrlDecoder();

  private final KeyMaterialService keyMaterial;
  private final ObjectMapper mapper;

  public AccessibilitySuggestionTokenService(KeyMaterialService keyMaterial, ObjectMapper mapper) {
    this.keyMaterial = Objects.requireNonNull(keyMaterial, "keyMaterial");
    this.mapper = Objects.requireNonNull(mapper, "mapper");
  }

  public String mint(long usuarioId, String suggestionHash, Duration ttl) {
    Objects.requireNonNull(suggestionHash, "suggestionHash");
    Duration t = (ttl == null) ? Duration.ofMinutes(15) : ttl;
    Instant exp = Instant.now().plus(t);
    Payload p = new Payload(usuarioId, suggestionHash, exp.toEpochMilli());
    String json = toJson(p);
    byte[] sig = hmac(json.getBytes(StandardCharsets.UTF_8), keyMaterial.getUiAccessibilitySuggestSigningKey());
    return B64U.encodeToString(json.getBytes(StandardCharsets.UTF_8)) + "." + B64U.encodeToString(sig);
  }

  public Verified verifyRequired(String token, long expectedUserId, String expectedHash) {
    Verified v = verify(token);
    if (!v.valid) {
      throw new IllegalArgumentException("invalid suggestion token");
    }
    if (v.usuarioId != expectedUserId) {
      throw new IllegalArgumentException("token user mismatch");
    }
    if (!Objects.equals(v.suggestionHash, expectedHash)) {
      throw new IllegalArgumentException("token hash mismatch");
    }
    return v;
  }

  public Verified verify(String token) {
    if (token == null || token.isBlank()) {
      return Verified.invalid("blank");
    }
    String t = token.trim();
    int dot = t.indexOf('.');
    if (dot <= 0 || dot + 1 >= t.length()) {
      return Verified.invalid("format");
    }
    try {
      byte[] payloadBytes = B64UD.decode(t.substring(0, dot));
      byte[] sigBytes = B64UD.decode(t.substring(dot + 1));
      String json = new String(payloadBytes, StandardCharsets.UTF_8);

      byte[] expected = hmac(json.getBytes(StandardCharsets.UTF_8), keyMaterial.getUiAccessibilitySuggestSigningKey());
      if (!constantTimeEquals(expected, sigBytes)) {
        return Verified.invalid("sig");
      }

      Payload p = mapper.readValue(json, Payload.class);
      if (p.expEpochMs <= 0 || Instant.ofEpochMilli(p.expEpochMs).isBefore(Instant.now())) {
        return Verified.invalid("expired");
      }
      if (p.usuarioId <= 0 || p.suggestionHash == null || p.suggestionHash.isBlank()) {
        return Verified.invalid("payload");
      }
      return new Verified(true, null, p.usuarioId, p.suggestionHash, p.expEpochMs);
    } catch (Exception e) {
      return Verified.invalid("decode");
    }
  }

  private String toJson(Payload p) {
    try {
      return mapper.writeValueAsString(p);
    } catch (Exception e) {
      throw new IllegalStateException("token json", e);
    }
  }

  private static byte[] hmac(byte[] message, SecretKey key) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key.getEncoded(), "HmacSHA256"));
      return mac.doFinal(message);
    } catch (Exception e) {
      throw new IllegalStateException("HmacSHA256", e);
    }
  }

  private static boolean constantTimeEquals(byte[] a, byte[] b) {
    if (a == null || b == null) return false;
    if (a.length != b.length) return false;
    int r = 0;
    for (int i = 0; i < a.length; i++) {
      r |= (a[i] ^ b[i]);
    }
    return r == 0;
  }

  private record Payload(long usuarioId, String suggestionHash, long expEpochMs) {
  }

  public record Verified(boolean valid, String reason, long usuarioId, String suggestionHash, long expEpochMs) {
    static Verified invalid(String reason) {
      return new Verified(false, reason, 0L, null, 0L);
    }
  }
}
