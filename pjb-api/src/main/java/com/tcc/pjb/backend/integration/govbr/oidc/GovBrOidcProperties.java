package com.tcc.pjb.backend.integration.govbr.oidc;

import java.net.URI;
import java.util.Locale;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;







@ConfigurationProperties(prefix = "pjb.integrations.govbr")
public record GovBrOidcProperties(
    boolean enabled,
    boolean mockEnabled,
    String authorizeUrl,
    String tokenUrl,
    String userinfoUrl,
    String pictureUrl,
    String clientId,
    String clientSecret,
    String redirectUri,
    String redirectUriStepUp,
    String redirectUriLogin,
    String stepUpScope,
    String jwksUrl,
    String issuer,
    String frontendSuccessRedirect,
    String frontendErrorRedirect,
    String frontendLoginSuccessRedirect,
    String frontendLoginErrorRedirect,
    Duration connectTimeout,
    Duration requestTimeout,
    Duration stateTtl
) {

  public GovBrOidcProperties {
    connectTimeout = defaultDuration(connectTimeout, Duration.ofSeconds(4));
    requestTimeout = defaultDuration(requestTimeout, Duration.ofSeconds(6));
    stateTtl = defaultDuration(stateTtl, Duration.ofMinutes(5));
  }


  public String effectiveCitizenLinkScope() {
    return "openid email profile govbr_confiabilidades";
  }

  public java.util.List<String> officialProductionDomainSuffixes() {
    return java.util.List.of(".gov.br", ".jus.br", ".mp.br", ".def.br", ".leg.br", ".mil.br", ".edu.br", ".tc.br");
  }

  public String effectiveStepUpScope() {
    String s = stepUpScope;
    if (s == null || s.isBlank()) {
      return "openid email profile govbr_confiabilidades";
    }
    return s.trim();
  }

  public String effectiveStepUpRedirectUri() {
    String u = redirectUriStepUp;
    if (u != null && !u.isBlank()) {
      return u.trim();
    }
    return redirectUri;
  }

  public String effectiveLoginRedirectUri() {
    String u = redirectUriLogin;
    if (u != null && !u.isBlank()) {
      return u.trim();
    }
    return redirectUri;
  }

  



  public void validateIfEnabled() {
    if (!enabled) return;
    requireNonBlank(authorizeUrl, "authorize-url");
    requireNonBlank(tokenUrl, "token-url");
    requireNonBlank(userinfoUrl, "userinfo-url");
    requireNonBlank(clientId, "client-id");
    requireNonBlank(redirectUri, "redirect-uri");

    
    
    if (!mockEnabled) {
      requireNonBlank(jwksUrl, "jwks-url");
      requireNonBlank(issuer, "issuer");
    }

    validateHttpsOrLoopback(authorizeUrl, "authorize-url", false);
    validateHttpsOrLoopback(tokenUrl, "token-url", false);
    validateHttpsOrLoopback(userinfoUrl, "userinfo-url", false);
    if (pictureUrl != null && !pictureUrl.isBlank()) {
      validateHttpsOrLoopback(pictureUrl, "picture-url", false);
    }
    if (jwksUrl != null && !jwksUrl.isBlank()) {
      validateHttpsOrLoopback(jwksUrl, "jwks-url", false);
    }
    validateHttpsOrLoopback(redirectUri, "redirect-uri", true);
    if (redirectUriStepUp != null && !redirectUriStepUp.isBlank()) {
      validateHttpsOrLoopback(redirectUriStepUp, "redirect-uri-step-up", true);
    }
    if (redirectUriLogin != null && !redirectUriLogin.isBlank()) {
      validateHttpsOrLoopback(redirectUriLogin, "redirect-uri-login", true);
    }
    if (frontendSuccessRedirect != null && !frontendSuccessRedirect.isBlank()) {
      validateHttpsOrLoopback(frontendSuccessRedirect, "frontend-success-redirect", true);
    }
    if (frontendErrorRedirect != null && !frontendErrorRedirect.isBlank()) {
      validateHttpsOrLoopback(frontendErrorRedirect, "frontend-error-redirect", true);
    }
  }


  private static void validateHttpsOrLoopback(String value, String name, boolean allowLoopbackHttp) {
    URI uri;
    try {
      uri = URI.create(value.trim()).normalize();
    } catch (Exception e) {
      throw new IllegalStateException("pjb.integrations.govbr." + name + " inválido", e);
    }
    String scheme = uri.getScheme() == null ? "" : uri.getScheme().trim().toLowerCase(Locale.ROOT);
    String host = uri.getHost() == null ? "" : uri.getHost().trim().toLowerCase(Locale.ROOT);
    boolean loopback = "localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    boolean validScheme = "https".equals(scheme) || (allowLoopbackHttp && loopback && "http".equals(scheme));
    if (!validScheme || host.isBlank() || uri.getUserInfo() != null || uri.getFragment() != null) {
      throw new IllegalStateException("pjb.integrations.govbr." + name + " deve usar HTTPS e host explícito; HTTP só é aceito em loopback controlado.");
    }
  }

  private static Duration defaultDuration(Duration v, Duration dflt) {
    if (v == null || v.isNegative() || v.isZero()) return dflt;
    return v;
  }

  private static void requireNonBlank(String v, String name) {
    if (v == null || v.isBlank()) {
      throw new IllegalStateException("pjb.integrations.govbr." + name + " is required");
    }
  }
}
