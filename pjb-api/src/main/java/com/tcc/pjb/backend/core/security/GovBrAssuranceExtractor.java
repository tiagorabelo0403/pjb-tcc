package com.tcc.pjb.backend.core.security;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class GovBrAssuranceExtractor {

    public String extract(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken token)) {
            return "bronze";
        }
        Jwt jwt = token.getToken();
        if (jwt == null) {
            return "bronze";
        }
        Map<String, Object> claims = jwt.getClaims();
        String explicit = firstText(
                claims.get("govbr_assurance_level"),
                claims.get("assurance_level"),
                claims.get("govbr_nivel"),
                claims.get("nivel"),
                claims.get("loa"),
                claims.get("acr"));
        String normalized = normalize(explicit);
        if (normalized != null) {
            return normalized;
        }
        Object amr = claims.get("amr");
        if (amr instanceof Collection<?> collection) {
            for (Object item : collection) {
                String candidate = normalize(Objects.toString(item, null));
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        return "bronze";
    }

    private String firstText(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String text = Objects.toString(value, null);
            if (text != null && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    private String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.contains("ouro") || value.contains("gold") || value.contains("loa3") || value.equals("3")) {
            return "ouro";
        }
        if (value.contains("prata") || value.contains("silver") || value.contains("loa2") || value.equals("2")) {
            return "prata";
        }
        if (value.contains("bronze") || value.contains("loa1") || value.equals("1")) {
            return "bronze";
        }
        return null;
    }
}
