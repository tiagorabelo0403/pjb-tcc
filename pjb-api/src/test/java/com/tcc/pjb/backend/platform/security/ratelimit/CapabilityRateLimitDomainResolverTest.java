package com.tcc.pjb.backend.platform.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.configs.security.PjbGrantedAuthorityFactory;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.authentication.TestingAuthenticationToken;

class CapabilityRateLimitDomainResolverTest {

    private final CapabilityRateLimitDomainResolver resolver = new CapabilityRateLimitDomainResolver();

    static Stream<Arguments> casosPorTipoUsuario() {
        return Stream.of(
                Arguments.of(TipoUsuario.CIDADAO, CapabilityRateLimitDomain.CITIZEN),
                Arguments.of(TipoUsuario.ADVOGADO, CapabilityRateLimitDomain.LAWYER),
                Arguments.of(TipoUsuario.JUIZ_ESTADUAL, CapabilityRateLimitDomain.INSTITUCIONAL),
                Arguments.of(TipoUsuario.DEFENSOR_PUBLICO, CapabilityRateLimitDomain.INSTITUCIONAL),
                Arguments.of(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO, CapabilityRateLimitDomain.INSTITUCIONAL),
                Arguments.of(TipoUsuario.PROCURADOR, CapabilityRateLimitDomain.INSTITUCIONAL),
                Arguments.of(TipoUsuario.PERITO, CapabilityRateLimitDomain.INSTITUCIONAL),
                Arguments.of(TipoUsuario.SERVIDOR_FORUM, CapabilityRateLimitDomain.INSTITUCIONAL)
        );
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("casosPorTipoUsuario")
    void resolveDominioComConjuntoRealDeAuthorities(TipoUsuario tipoUsuario, CapabilityRateLimitDomain esperado) {
        List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities =
                PjbGrantedAuthorityFactory.authoritiesFor(tipoUsuario, null);
        var authentication = new TestingAuthenticationToken("user", "pwd", authorities);

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
