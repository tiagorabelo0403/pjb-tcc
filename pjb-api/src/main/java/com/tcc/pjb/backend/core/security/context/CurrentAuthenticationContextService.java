package com.tcc.pjb.backend.core.security.context;

import com.tcc.pjb.backend.core.security.stepup.JwtStepUpClaims;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class CurrentAuthenticationContextService {

    public CurrentAuthenticationContext current() {
        return current(SecurityContextHolder.getContext() == null ? null : SecurityContextHolder.getContext().getAuthentication());
    }

    public CurrentAuthenticationContext current(Authentication authentication) {
        if (authentication == null) {
            return anonymous();
        }
        boolean jwtBacked = authentication instanceof JwtAuthenticationToken;
        boolean authenticated = authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken);
        String authenticationType = authentication.getClass().getSimpleName();
        String principalName = trim(authentication.getName());
        String authenticationMethod = jwtBacked ? "JWT_BEARER" : authenticated ? "SESSION_AUTH" : "ANONIMO";
        String principalSubject = null;
        String principalIssuer = null;
        String principalUid = null;
        String principalCpf = null;
        String principalEmail = null;
        String acr = null;
        List<String> amr = List.of();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            Jwt jwt = jwtAuthenticationToken.getToken();
            if (jwt != null) {
                principalSubject = trim(jwt.getSubject());
                principalIssuer = jwt.getIssuer() == null ? null : trim(jwt.getIssuer().toString());
                principalUid = claim(jwt, "uid");
                principalCpf = claim(jwt, "cpf");
                principalEmail = claim(jwt, "email");
                acr = claim(jwt, "acr");
                amr = normalizeAmr(jwt.getClaims().get("amr"));
                authenticationMethod = resolveAuthenticationMethod(amr, acr);
                if (principalName == null) {
                    principalName = firstNonBlank(claim(jwt, "preferred_username"), principalEmail, principalCpf, principalSubject);
                }
            }
        }
        List<String> authorities = authentication.getAuthorities() == null
                ? List.of()
                : authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).filter(item -> item != null && !item.isBlank()).distinct().toList();
        return new CurrentAuthenticationContext(
                true,
                authenticated,
                jwtBacked,
                authenticationType,
                authenticationMethod,
                principalName,
                principalSubject,
                principalIssuer,
                principalUid,
                principalCpf,
                principalEmail,
                acr,
                amr,
                authorities,
                JwtStepUpClaims.hasMfa(authentication),
                Instant.now());
    }

    private CurrentAuthenticationContext anonymous() {
        return new CurrentAuthenticationContext(false, false, false, "ANONIMO", "ANONIMO", null, null, null, null, null, null, null, List.of(), List.of(), false, Instant.now());
    }

    private String resolveAuthenticationMethod(List<String> amr, String acr) {
        if (amr.stream().anyMatch(item -> containsToken(item, "passkey") || containsToken(item, "webauthn"))) {
            return "PASSKEY";
        }
        if (amr.stream().anyMatch(item -> containsToken(item, "cert") || containsToken(item, "x509"))) {
            return "CERTIFICADO";
        }
        if (amr.stream().anyMatch(item -> containsToken(item, "mfa") || containsToken(item, "otp") || containsToken(item, "totp"))) {
            return "JWT_MFA";
        }
        if (containsToken(acr, "govbr") || containsToken(acr, "prata") || containsToken(acr, "ouro") || containsToken(acr, "loa2") || containsToken(acr, "loa3")) {
            return "GOVBR";
        }
        return "JWT_BEARER";
    }

    private List<String> normalizeAmr(Object raw) {
        if (raw == null) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (raw instanceof Collection<?> collection) {
            for (Object item : collection) {
                addToken(out, item);
            }
        } else if (raw instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                addToken(out, item);
            }
        } else {
            String text = String.valueOf(raw);
            if (text.contains(",")) {
                for (String token : text.split(",")) {
                    addToken(out, token);
                }
            } else {
                addToken(out, raw);
            }
        }
        return List.copyOf(out);
    }

    private void addToken(LinkedHashSet<String> out, Object raw) {
        if (raw == null) {
            return;
        }
        String token = trim(String.valueOf(raw));
        if (token != null) {
            out.add(token);
        }
    }

    private boolean containsToken(String raw, String token) {
        return raw != null && token != null && raw.toLowerCase(Locale.ROOT).contains(token.toLowerCase(Locale.ROOT));
    }

    private String claim(Jwt jwt, String name) {
        if (jwt == null || name == null || name.isBlank()) {
            return null;
        }
        Object value = jwt.getClaims().get(name);
        return trim(value == null ? null : String.valueOf(value));
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String candidate = trim(value);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
