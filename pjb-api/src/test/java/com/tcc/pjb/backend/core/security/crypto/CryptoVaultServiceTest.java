package com.tcc.pjb.backend.core.security.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class CryptoVaultServiceTest {

    private static final String CHAVE_VALIDA = Base64.getEncoder().encodeToString(
            "12345678901234567890123456789012".getBytes());

    @Test
    void blindarELerRoundTripComChaveReal() {
        CryptoVaultService vault = new CryptoVaultService(CHAVE_VALIDA, false);

        String cifrado = vault.blindarDado("12345678901");

        assertThat(cifrado).isNotEqualTo("12345678901");
        assertThat(vault.lerDadoBlindado(cifrado)).isEqualTo("12345678901");
    }

    @Test
    void blindarProduzCifradoDiferenteACadaChamadaMesmoParaOMesmoValor() {
        CryptoVaultService vault = new CryptoVaultService(CHAVE_VALIDA, false);

        String c1 = vault.blindarDado("12345678901");
        String c2 = vault.blindarDado("12345678901");

        assertThat(c1).isNotEqualTo(c2);
        assertThat(vault.lerDadoBlindado(c1)).isEqualTo("12345678901");
        assertThat(vault.lerDadoBlindado(c2)).isEqualTo("12345678901");
    }

    @Test
    void hmacHexEhDeterministicoParaOMesmoValor() {
        CryptoVaultService vault = new CryptoVaultService(CHAVE_VALIDA, false);

        String h1 = vault.hmacHex("12345678901");
        String h2 = vault.hmacHex("12345678901");

        assertThat(h1).isEqualTo(h2).hasSize(64);
    }

    @Test
    void hmacHexDifereParaValoresDiferentes() {
        CryptoVaultService vault = new CryptoVaultService(CHAVE_VALIDA, false);

        assertThat(vault.hmacHex("12345678901")).isNotEqualTo(vault.hmacHex("12345678902"));
    }

    @Test
    void semChaveESemFallbackLanca() {
        CryptoVaultService vault = new CryptoVaultService("", false);

        assertThatThrownBy(() -> vault.blindarDado("x")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> vault.hmacHex("x")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void semChaveComFallbackDegradaDeFormaSeguraENuncaLancaESeguerReversivel() {
        CryptoVaultService vault = new CryptoVaultService("", true);

        String cifrado = vault.blindarDado("12345678901");
        assertThat(vault.lerDadoBlindado(cifrado)).isEqualTo("12345678901");

        String hash = vault.hmacHex("12345678901");
        assertThat(hash).hasSize(64);
        assertThat(vault.hmacHex("12345678901")).isEqualTo(hash);
    }
}
