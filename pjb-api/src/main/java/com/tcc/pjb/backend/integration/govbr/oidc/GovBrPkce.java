package com.tcc.pjb.backend.integration.govbr.oidc;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;




public final class GovBrPkce {

  private static final ThreadLocal<SecureRandom> RNG = ThreadLocal.withInitial(SecureRandom::new);

  private GovBrPkce() {
  }

  public static Generated generate() {
    return generate(48, 24);
  }

  public static Generated generate(int verifierBytes, int nonceBytes) {
    SecureRandom rng = RNG.get();
    String codeVerifier = randomB64Url(rng, verifierBytes);
    String codeChallenge = GovBrOidcClient.base64Url(
        GovBrOidcClient.sha256(codeVerifier.getBytes(StandardCharsets.US_ASCII)));
    String nonce = randomB64Url(rng, nonceBytes);
    return new Generated(codeVerifier, codeChallenge, nonce);
  }

  public record Generated(String codeVerifier, String codeChallenge, String nonce) {
  }

  private static String randomB64Url(SecureRandom rng, int lenBytes) {
    byte[] b = new byte[Math.max(16, lenBytes)];
    rng.nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }
}
