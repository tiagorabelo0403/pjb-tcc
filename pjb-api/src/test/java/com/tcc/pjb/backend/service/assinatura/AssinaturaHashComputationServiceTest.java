package com.tcc.pjb.backend.service.assinatura;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssinaturaHashComputationServiceTest {

    private final AssinaturaHashComputationService service = new AssinaturaHashComputationService();

    @Test
    void computarSha256_retornaHashValido() {
        byte[] conteudo = "documento judicial".getBytes();
        var result = service.computarSha256(conteudo);
        assertThat(result.algoritmo()).isEqualTo("SHA-256");
        assertThat(result.hashHex()).hasSize(64);
        assertThat(result.tamanhoBytes()).isEqualTo(conteudo.length);
    }

    @Test
    void computarSha512_retornaHashValido() {
        byte[] conteudo = "documento judicial".getBytes();
        var result = service.computarSha512(conteudo);
        assertThat(result.algoritmo()).isEqualTo("SHA-512");
        assertThat(result.hashHex()).hasSize(128);
    }

    @Test
    void verificarIntegridade_retornaTrue_quandoHashConfere() {
        byte[] conteudo = "petição assinada".getBytes();
        var hash = service.computarSha256(conteudo);
        assertThat(service.verificarIntegridade(conteudo, hash.hashHex(), "SHA-256")).isTrue();
    }

    @Test
    void verificarIntegridade_retornaFalse_quandoHashDivergente() {
        byte[] original = "petição original".getBytes();
        byte[] adulterado = "petição adulterada".getBytes();
        var hash = service.computarSha256(original);
        assertThat(service.verificarIntegridade(adulterado, hash.hashHex(), "SHA-256")).isFalse();
    }

    @Test
    void computarDeTexto_mesmoResultadoQueBytes() {
        String texto = "sentença";
        var porTexto = service.computarDeTexto(texto, "SHA-256");
        var porBytes = service.computarSha256(texto.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(porTexto.hashHex()).isEqualTo(porBytes.hashHex());
    }

    @Test
    void computar_algortimoInvalido_lancaException() {
        assertThatThrownBy(() -> service.computarDeTexto("texto", "INVALIDO-999"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não suportado");
    }

    @Test
    void hashDeterministico_mesmaEntradaMesmoHash() {
        byte[] conteudo = "acórdão".getBytes();
        assertThat(service.computarSha256(conteudo).hashHex())
                .isEqualTo(service.computarSha256(conteudo).hashHex());
    }
}
