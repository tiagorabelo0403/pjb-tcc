package com.tcc.pjb.backend.integration.govbr.oidc;

import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.tcc.pjb.backend.core.util.Hashes;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class GovBrOidcClient {

  private static final Duration CLOCK_SKEW = Duration.ofMinutes(2);

  private final HttpClient http;
  private final ObjectMapper om;
  private final GovBrOidcProperties props;

  private final AtomicReference<JwksCache> jwksCache = new AtomicReference<>();

  public GovBrOidcClient(ObjectMapper objectMapper, GovBrOidcProperties props, HttpClient httpClient) {
    this.om = Objects.requireNonNull(objectMapper);
    this.props = Objects.requireNonNull(props);
    this.http = Objects.requireNonNull(httpClient);
  }

  @CircuitBreaker(name = "govbr-oidc")
  @Retry(name = "govbr-oidc")
  @Bulkhead(name = "govbr-oidc")
  public GovBrTokenResponse exchangeCode(String code, String codeVerifier) throws IOException, InterruptedException {
    return exchangeCode(code, codeVerifier, props.redirectUri());
  }

  public GovBrTokenResponse exchangeCode(String code, String codeVerifier, String redirectUri) throws IOException, InterruptedException {
    Map<String, String> form = new LinkedHashMap<>();
    form.put("grant_type", "authorization_code");
    form.put("code", code);
    form.put("redirect_uri", redirectUri);
    form.put("client_id", props.clientId());
    if (props.clientSecret() != null && !props.clientSecret().isBlank()) {
      form.put("client_secret", props.clientSecret());
    }
    form.put("code_verifier", codeVerifier);

    HttpRequest req = HttpRequest.newBuilder(URI.create(props.tokenUrl()))
        .timeout(props.requestTimeout())
        .header("User-Agent", "PJB/1.0 (govbr)")
        .header("Accept", "application/json")
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(urlEncode(form)))
        .build();

    HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
      throw new IOException("govbr_token_http_" + resp.statusCode());
    }
    return om.readValue(resp.body(), GovBrTokenResponse.class);
  }

  @CircuitBreaker(name = "govbr-oidc")
  @Retry(name = "govbr-oidc")
  @Bulkhead(name = "govbr-oidc")
  public GovBrUserInfoResponse userInfo(String accessToken) throws IOException, InterruptedException {
    HttpRequest req = HttpRequest.newBuilder(URI.create(props.userinfoUrl()))
        .timeout(props.requestTimeout())
        .header("User-Agent", "PJB/1.0 (govbr)")
        .header("Accept", "application/json")
        .header("Authorization", "Bearer " + accessToken)
        .GET()
        .build();

    HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
      throw new IOException("govbr_userinfo_http_" + resp.statusCode());
    }
    return om.readValue(resp.body(), GovBrUserInfoResponse.class);
  }

  @CircuitBreaker(name = "govbr-oidc")
  @Retry(name = "govbr-oidc")
  @Bulkhead(name = "govbr-oidc")
  public byte[] userPictureBase64(String accessToken) throws IOException, InterruptedException {
    if (props.pictureUrl() == null || props.pictureUrl().isBlank()) {
      throw new IOException("govbr_picture_url_missing");
    }

    HttpRequest req = HttpRequest.newBuilder(URI.create(props.pictureUrl()))
        .timeout(props.requestTimeout())
        .header("User-Agent", "PJB/1.0 (govbr)")
        .header("Accept", "text/plain, */*")
        .header("Authorization", "Bearer " + accessToken)
        .GET()
        .build();

    HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
      throw new IOException("govbr_picture_http_" + resp.statusCode());
    }
    String body = resp.body();
    if (body == null || body.isBlank()) {
      throw new IOException("govbr_picture_empty");
    }
    return Base64.getDecoder().decode(body.trim());
  }

  public VerifiedIdToken parseAndVerifyIdToken(String rawIdToken) throws IOException, InterruptedException {
    if (rawIdToken == null || rawIdToken.isBlank()) {
      throw new IOException("id_token_missing");
    }
    try {
      SignedJWT jwt = SignedJWT.parse(rawIdToken.trim());
      JWK key = resolveKey(jwt.getHeader());
      JWSVerifier verifier = buildVerifier(key, jwt.getHeader().getAlgorithm());
      if (!jwt.verify(verifier)) {
        throw new IOException("id_token_signature_invalid");
      }
      JWTClaimsSet claims = jwt.getJWTClaimsSet();
      validateIdTokenClaims(claims);
      return new VerifiedIdToken(
          claims,
          jwt.getHeader().getKeyID(),
          toInstant(claims.getIssueTime()),
          toInstant(claims.getExpirationTime()),
          claims.getAudience() == null ? List.of() : List.copyOf(claims.getAudience())
      );
    } catch (IOException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IOException("id_token_parse_or_verify_failed", ex);
    }
  }

  public GovBrAccessTokenSignals extractAccessTokenSignals(String accessToken) {
    if (accessToken == null || accessToken.isBlank()) {
      return GovBrAccessTokenSignals.empty();
    }
    try {
      String[] parts = accessToken.trim().split("\\.");
      if (parts.length < 2) {
        return GovBrAccessTokenSignals.empty();
      }
      byte[] payload = Base64.getUrlDecoder().decode(padBase64(parts[1]));
      Map<String, Object> claims = om.readValue(payload, new TypeReference<Map<String, Object>>() {});
      List<String> amr = extractStringList(claims.get("amr"));
      boolean mfaPresent = amr.stream().anyMatch(this::isStrongAuthSignal);
      return new GovBrAccessTokenSignals(
          true,
          mfaPresent,
          amr,
          stringValue(claims.get("iss")),
          stringValue(claims.get("sub")),
          parseEpochSeconds(claims.get("exp")),
          Map.copyOf(claims),
          stringValue(claims.get("acr"))
      );
    } catch (Exception ignored) {
      return GovBrAccessTokenSignals.empty();
    }
  }

  @CircuitBreaker(name = "govbr-jwks")
  @Retry(name = "govbr-jwks")
  @Bulkhead(name = "govbr-jwks")
  public JWKSet fetchJwks() throws IOException, InterruptedException {
    JwksCache cached = jwksCache.get();
    Instant now = Instant.now();
    if (cached != null && now.isBefore(cached.expiresAt())) {
      return cached.jwkSet();
    }
    HttpRequest req = HttpRequest.newBuilder(URI.create(props.jwksUrl()))
        .timeout(props.requestTimeout())
        .header("User-Agent", "PJB/1.0 (govbr)")
        .header("Accept", "application/json")
        .GET()
        .build();

    HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
      throw new IOException("jwks_http_" + resp.statusCode());
    }
    try {
      JWKSet set = JWKSet.parse(resp.body());
      jwksCache.set(new JwksCache(set, now.plus(Duration.ofMinutes(15))));
      return set;
    } catch (Exception e) {
      throw new IOException("jwks_parse", e);
    }
  }

  public JWK resolveKey(JWSHeader header) throws IOException, InterruptedException {
    JWKSet set = fetchJwks();
    String kid = header.getKeyID();
    if (kid != null) {
      JWK key = set.getKeyByKeyId(kid);
      if (key != null) return key;
    }
    List<JWK> keys = set.getKeys();
    if (keys.isEmpty()) throw new IOException("jwks_empty");
    return keys.getFirst();
  }

  private JWSVerifier buildVerifier(JWK key, JWSAlgorithm algorithm) throws IOException, JOSEException {
    if (key instanceof RSAKey rsa) {
      return new RSASSAVerifier(rsa.toRSAPublicKey());
    }
    if (key instanceof ECKey ec) {
      return new ECDSAVerifier(ec.toECPublicKey());
    }
    if (key instanceof OctetSequenceKey oct && algorithm != null && algorithm.getName().startsWith("HS")) {
      return new MACVerifier(oct.toByteArray());
    }
    throw new IOException("jwks_key_type_unsupported");
  }

  private void validateIdTokenClaims(JWTClaimsSet claims) throws IOException {
    if (claims == null) {
      throw new IOException("id_token_claims_missing");
    }
    String subject = claims.getSubject();
    if (subject == null || subject.isBlank()) {
      throw new IOException("id_token_subject_missing");
    }
    List<String> audience = claims.getAudience();
    if (audience == null || audience.stream().noneMatch(props.clientId()::equals)) {
      throw new IOException("id_token_audience_mismatch");
    }
    String issuer = claims.getIssuer();
    if (props.issuer() != null && !props.issuer().isBlank() && !Objects.equals(props.issuer().trim(), issuer)) {
      throw new IOException("id_token_issuer_mismatch");
    }
    Instant now = Instant.now();
    Instant expiration = toInstant(claims.getExpirationTime());
    if (expiration == null || now.isAfter(expiration.plus(CLOCK_SKEW))) {
      throw new IOException("id_token_expired");
    }
    Instant issuedAt = toInstant(claims.getIssueTime());
    if (issuedAt != null && issuedAt.isAfter(now.plus(CLOCK_SKEW))) {
      throw new IOException("id_token_issued_in_future");
    }
    Instant notBefore = toInstant(claims.getNotBeforeTime());
    if (notBefore != null && notBefore.isAfter(now.plus(CLOCK_SKEW))) {
      throw new IOException("id_token_not_before");
    }
  }

  private boolean isStrongAuthSignal(String value) {
    if (value == null) {
      return false;
    }
    String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
    return normalized.equals("mfa") || normalized.equals("otp") || normalized.equals("fido") || normalized.equals("face") || normalized.equals("certificate");
  }

  private List<String> extractStringList(Object value) {
    LinkedHashSet<String> out = new LinkedHashSet<>();
    if (value instanceof Iterable<?> iterable) {
      for (Object item : iterable) {
        String text = stringValue(item);
        if (text != null) {
          out.add(text.toLowerCase(java.util.Locale.ROOT));
        }
      }
    } else {
      String text = stringValue(value);
      if (text != null) {
        out.add(text.toLowerCase(java.util.Locale.ROOT));
      }
    }
    return out.isEmpty() ? List.of() : new ArrayList<>(out);
  }

  private Instant parseEpochSeconds(Object value) {
    if (value == null) {
      return null;
    }
    try {
      long seconds = Long.parseLong(String.valueOf(value).trim());
      return Instant.ofEpochSecond(seconds);
    } catch (Exception ignored) {
      return null;
    }
  }

  private static Instant toInstant(java.util.Date value) {
    return value == null ? null : value.toInstant();
  }

  private String stringValue(Object value) {
    if (value == null) {
      return null;
    }
    String text = String.valueOf(value).trim();
    return text.isBlank() ? null : text;
  }

  private static String padBase64(String value) {
    int remainder = value.length() % 4;
    if (remainder == 0) {
      return value;
    }
    return value + "=".repeat(4 - remainder);
  }

  private record JwksCache(JWKSet jwkSet, Instant expiresAt) {
  }

  private static String urlEncode(Map<String, String> form) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (Map.Entry<String, String> e : form.entrySet()) {
      if (!first) sb.append('&');
      first = false;
      sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8));
      sb.append('=');
      sb.append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
    }
    return sb.toString();
  }

  public static String base64Url(byte[] bytes) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public static byte[] sha256(byte[] bytes) {
    byte[] digest = Hashes.sha256(bytes);
    if (digest.length == 0) {
      throw new IllegalStateException("sha256_unavailable");
    }
    return digest;
  }

  public record VerifiedIdToken(
      JWTClaimsSet claims,
      String kid,
      Instant issuedAt,
      Instant expiresAt,
      List<String> audience
  ) {
    public VerifiedIdToken {
      audience = audience == null ? List.of() : List.copyOf(audience);
    }
  }

  public record GovBrAccessTokenSignals(
      boolean jwtLike,
      boolean mfaPresent,
      List<String> amr,
      String issuer,
      String subject,
      Instant expiresAt,
      Map<String, Object> claims,
      String acr
  ) {
    public GovBrAccessTokenSignals {
      amr = amr == null ? List.of() : List.copyOf(amr);
      claims = claims == null ? Map.of() : Map.copyOf(claims);
    }

    public static GovBrAccessTokenSignals empty() {
      return new GovBrAccessTokenSignals(false, false, List.of(), null, null, null, Map.of(), null);
    }
  }
}
