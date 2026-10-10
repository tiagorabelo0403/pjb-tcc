package com.tcc.pjb.backend.configs.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class BearerDoEmissorExternoResolverTest {

    private final BearerDoEmissorExternoResolver resolver = new BearerDoEmissorExternoResolver();

    @Test
    void entregaAoValidadorJwtOTokenComFormatoDeJwt() {
        String jwt = segmento("{\"alg\":\"RS256\"}") + "." + segmento("{\"sub\":\"usuario\"}") + "." + segmento("assinatura");

        assertThat(resolver.resolve(comAutorizacao("Bearer " + jwt))).isEqualTo(jwt);
    }

    @Test
    void deixaPassarOTokenOpacoDaSessaoParaOFiltroDaSessao() {
        String sessaoOpaca = "q0Z8m1XrKcW7v3pT9yL2sB6nD4fH5jA8eR1uI0oP3kM";

        assertThat(resolver.resolve(comAutorizacao("Bearer " + sessaoOpaca))).isNull();
    }

    @Test
    void deixaOTokenDoMarketplaceParaAAutenticacaoDoProprioMarketplace() {
        String jwt = segmento("{\"alg\":\"HS256\",\"typ\":\"at+jwt\"}") + "." + segmento("{\"sub\":\"cliente\"}") + "." + segmento("assinatura");
        MockHttpServletRequest request = comAutorizacao("Bearer " + jwt);
        request.setRequestURI("/api/marketplace/v1/processos");

        assertThat(resolver.resolve(request)).isNull();
    }

    @Test
    void semCabecalhoBearerNaoResolveNada() {
        assertThat(resolver.resolve(new MockHttpServletRequest())).isNull();
        assertThat(resolver.resolve(comAutorizacao("Basic dXN1YXJpbzpzZW5oYQ=="))).isNull();
    }

    private static String segmento(String conteudo) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(conteudo.getBytes(StandardCharsets.UTF_8));
    }

    private static MockHttpServletRequest comAutorizacao(String valor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", valor);
        return request;
    }
}
