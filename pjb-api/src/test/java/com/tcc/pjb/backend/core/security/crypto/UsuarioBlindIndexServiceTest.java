package com.tcc.pjb.backend.core.security.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class UsuarioBlindIndexServiceTest {

    private final CryptoVaultService vault = new CryptoVaultService(
            Base64.getEncoder().encodeToString("12345678901234567890123456789012".getBytes()), false);
    private final UsuarioBlindIndexService blindIndex = new UsuarioBlindIndexService(vault);

    @Test
    void hashCpfIgnoraFormatacaoDiferente() {
        assertThat(blindIndex.hashCpf("123.456.789-01")).isEqualTo(blindIndex.hashCpf("12345678901"));
    }

    @Test
    void hashEmailIgnoraCaixaEEspacos() {
        assertThat(blindIndex.hashEmail("  Usuario@Example.Com  ")).isEqualTo(blindIndex.hashEmail("usuario@example.com"));
    }

    @Test
    void hashCpfDeCpfsDiferentesNuncaColide() {
        assertThat(blindIndex.hashCpf("11111111111")).isNotEqualTo(blindIndex.hashCpf("22222222222"));
    }

    @Test
    void valorNuloOuVazioResultaEmHashNulo() {
        assertThat(blindIndex.hashCpf(null)).isNull();
        assertThat(blindIndex.hashCpf("")).isNull();
        assertThat(blindIndex.hashEmail(null)).isNull();
        assertThat(blindIndex.hashEmail("   ")).isNull();
    }

    @Test
    void normalizarCpfRemoveTudoQueNaoEhDigito() {
        assertThat(UsuarioBlindIndexService.normalizarCpf("123.456.789-01")).isEqualTo("12345678901");
    }

    @Test
    void normalizarEmailAplicaTrimEMinusculas() {
        assertThat(UsuarioBlindIndexService.normalizarEmail("  Fulano@Example.ORG ")).isEqualTo("fulano@example.org");
    }
}
