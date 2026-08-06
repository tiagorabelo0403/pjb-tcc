package com.tcc.pjb.backend.platform.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class CapabilityRateLimitDomainResolverTest {

    private final CapabilityRateLimitDomainResolver resolver = new CapabilityRateLimitDomainResolver();

    static Stream<Arguments> casos() {
        return Stream.of(
                Arguments.of("ROLE_ADVOGADO", CapabilityRateLimitDomain.LAWYER),
                Arguments.of("ROLE_ADVOCACIA", CapabilityRateLimitDomain.LAWYER),
                Arguments.of("ROLE_CIDADAO", CapabilityRateLimitDomain.CITIZEN),
                Arguments.of("ROLE_USER", CapabilityRateLimitDomain.CITIZEN),
                Arguments.of("ROLE_DEFENSOR_PUBLICO", CapabilityRateLimitDomain.INSTITUCIONAL),
                Arguments.of("ROLE_MEMBRO_MINISTERIO_PUBLICO", CapabilityRateLimitDomain.INSTITUCIONAL),
                Arguments.of("ROLE_PROCURADOR", CapabilityRateLimitDomain.INSTITUCIONAL),
                Arguments.of("ROLE_PERITO", CapabilityRateLimitDomain.INSTITUCIONAL),
                Arguments.of("ROLE_JUIZ_ESTADUAL", CapabilityRateLimitDomain.INSTITUCIONAL),
                Arguments.of("ROLE_SERVIDOR_FORUM", CapabilityRateLimitDomain.INSTITUCIONAL)
        );
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("casos")
    void resolveDominioPorAuthority(String authority, CapabilityRateLimitDomain esperado) {
        var authentication = new TestingAuthenticationToken("user", "pwd", List.of(new SimpleGrantedAuthority(authority)));

        assertThat(resolver.resolve(authentication)).isEqualTo(esperado);
    }

    @Test
    void resolveCitizenQuandoAuthenticationNula() {
        assertThat(resolver.resolve(null)).isEqualTo(CapabilityRateLimitDomain.CITIZEN);
    }

    @Test
    void resolveInstitucionalQuandoAuthenticationSemNenhumaAuthority() {
        var authentication = new TestingAuthenticationToken("user", "pwd", List.of());

        assertThat(resolver.resolve(authentication)).isEqualTo(CapabilityRateLimitDomain.INSTITUCIONAL);
    }
}
