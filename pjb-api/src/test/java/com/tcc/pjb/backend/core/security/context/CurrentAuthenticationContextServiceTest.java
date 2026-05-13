package com.tcc.pjb.backend.core.security.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class CurrentAuthenticationContextServiceTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void mustExtractJwtClaimsAuthoritiesAndMfaSignals() {
        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of(
                        "sub", "42",
                        "uid", "42",
                        "cpf", "12345678900",
                        "email", "servidor@tjce.jus.br",
                        "acr", "govbr_prata_loa2",
                        "amr", List.of("pwd", "mfa", "passkey")));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_SERVIDOR"), new SimpleGrantedAuthority("ROLE_PJB_INSTITUCIONAL"))));

        CurrentAuthenticationContext result = new CurrentAuthenticationContextService().current();

        assertThat(result.authenticationPresent()).isTrue();
        assertThat(result.authenticated()).isTrue();
        assertThat(result.jwtBacked()).isTrue();
        assertThat(result.authenticationMethod()).isEqualTo("PASSKEY");
        assertThat(result.principalSubject()).isEqualTo("42");
        assertThat(result.principalUid()).isEqualTo("42");
        assertThat(result.principalCpf()).isEqualTo("12345678900");
        assertThat(result.principalEmail()).isEqualTo("servidor@tjce.jus.br");
        assertThat(result.acr()).isEqualTo("govbr_prata_loa2");
        assertThat(result.amr()).containsExactly("pwd", "mfa", "passkey");
        assertThat(result.authorities()).contains("ROLE_SERVIDOR", "ROLE_PJB_INSTITUCIONAL");
        assertThat(result.mfaActive()).isTrue();
    }
}
