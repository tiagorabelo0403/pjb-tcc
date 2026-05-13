package com.tcc.pjb.backend.core.security.stepup;

import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;






public final class JwtStepUpClaims {

  private JwtStepUpClaims() {}

  public static boolean hasMfa() {
    return hasMfa(SecurityContextHolder.getContext().getAuthentication());
  }

  public static boolean hasMfa(Authentication auth) {
    if (!(auth instanceof JwtAuthenticationToken jat)) return false;
    Jwt jwt = jat.getToken();
    if (jwt == null) return false;

    
    Object amr = jwt.getClaims().get("amr");
    if (amr instanceof Iterable<?> it) {
      for (Object o : it) {
        if (o == null) continue;
        String s = String.valueOf(o).toLowerCase(Locale.ROOT);
        if (containsMfaToken(s)) return true;
      }
    } else if (amr != null) {
      String s = String.valueOf(amr).toLowerCase(Locale.ROOT);
      if (containsMfaToken(s)) return true;
    }

    
    Object acr = jwt.getClaims().get("acr");
    if (acr != null) {
      String s = String.valueOf(acr).toLowerCase(Locale.ROOT);
      if (s.contains("loa2") || s.contains("loa3") || s.equals("2") || s.equals("3")) {
        return true;
      }
    }

    return false;
  }

  private static boolean containsMfaToken(String s) {
    if (s == null) return false;
    return s.contains("mfa") || s.contains("webauthn") || s.contains("passkey") || s.contains("otp");
  }
}
