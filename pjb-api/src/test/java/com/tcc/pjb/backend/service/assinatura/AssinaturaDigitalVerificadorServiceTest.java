package com.tcc.pjb.backend.service.assinatura;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AssinaturaDigitalVerificadorServiceTest {

    private final AssinaturaDigitalVerificadorService service = new AssinaturaDigitalVerificadorService();

    @Test
    void verificar_valida_quandoIcpBrasilCompleto() {
        var result = service.verificar(inputValido());
        assertThat(result.valida()).isTrue();
        assertThat(result.falhas()).isEmpty();
        assertThat(result.nivel()).isEqualTo("A3");
    }

    @Test
    void verificar_invalida_quandoNaoIcpBrasil() {
        var input = new AssinaturaDigitalVerificadorService.AssinaturaInput(
                UUID.randomUUID(), "12345678901", "CERT123", "AC Teste",
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusYears(1),
                false, true, true, true, true);
        var result = service.verificar(input);
        assertThat(result.valida()).isFalse();
        assertThat(result.falhas()).anyMatch(f -> f.contains("ICP-Brasil"));
        assertThat(result.nivel()).isEqualTo("INVALIDO");
    }

    @Test
    void verificar_invalida_quandoCertificadoExpirado() {
        LocalDateTime assinatura = LocalDateTime.now();
        LocalDateTime validade = assinatura.minusDays(1);
        var input = new AssinaturaDigitalVerificadorService.AssinaturaInput(
                UUID.randomUUID(), "12345678901", "CERT123", "AC Serpro",
                assinatura, validade,
                true, true, true, true, true);
        var result = service.verificar(input);
        assertThat(result.valida()).isFalse();
        assertThat(result.falhas()).anyMatch(f -> f.contains("expirado"));
    }

    @Test
    void verificar_invalida_quandoHashDivergente() {
        var input = new AssinaturaDigitalVerificadorService.AssinaturaInput(
                UUID.randomUUID(), "12345678901", "CERT123", "AC Serpro",
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusYears(1),
                true, true, false, true, true);
        var result = service.verificar(input);
        assertThat(result.valida()).isFalse();
        assertThat(result.falhas()).anyMatch(f -> f.contains("adulteração"));
    }

    @Test
    void verificar_invalida_quandoSemCarimboTempo() {
        var input = new AssinaturaDigitalVerificadorService.AssinaturaInput(
                UUID.randomUUID(), "12345678901", "CERT123", "AC Serpro",
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusYears(1),
                true, true, true, true, false);
        var result = service.verificar(input);
        assertThat(result.valida()).isFalse();
        assertThat(result.falhas()).anyMatch(f -> f.contains("Carimbo de tempo"));
    }

    private AssinaturaDigitalVerificadorService.AssinaturaInput inputValido() {
        return new AssinaturaDigitalVerificadorService.AssinaturaInput(
                UUID.randomUUID(), "12345678901", "CERT-VALID-001", "AC Serpro",
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusYears(2),
                true, true, true, true, true);
    }
}
