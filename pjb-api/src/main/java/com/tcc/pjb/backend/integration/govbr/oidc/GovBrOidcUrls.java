package com.tcc.pjb.backend.integration.govbr.oidc;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;







public final class GovBrOidcUrls {

  private GovBrOidcUrls() {
  }

  public static String authorizeUrl(
      GovBrOidcProperties props,
      String redirectUri,
      String scope,
      String state,
      String codeChallenge,
      String nonce
  ) {
    String base = props.authorizeUrl();
    StringBuilder sb = new StringBuilder();
    sb.append(base);
    sb.append(base != null && base.contains("?") ? "&" : "?");
    sb.append("response_type=code");
    sb.append("&client_id=").append(url(props.clientId()));
    sb.append("&redirect_uri=").append(url(redirectUri));
    sb.append("&scope=").append(url(scope));
    sb.append("&state=").append(url(state));
    sb.append("&code_challenge=").append(url(codeChallenge));
    sb.append("&code_challenge_method=S256");
    sb.append("&nonce=").append(url(nonce));
    return sb.toString();
  }

  private static String url(String v) {
    return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
  }
}
