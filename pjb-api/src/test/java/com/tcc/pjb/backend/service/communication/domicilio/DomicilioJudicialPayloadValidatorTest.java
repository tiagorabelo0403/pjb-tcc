package com.tcc.pjb.backend.service.communication.domicilio;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DomicilioJudicialPayloadValidatorTest {

    private final DomicilioJudicialPayloadValidator validator = newValidator();

    private static DomicilioJudicialPayloadValidator newValidator() {
        try {
            var c = DomicilioJudicialPayloadValidator.class.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    @Test
    void validar_valido_quandoTudoPreenchido() {
        var input = new DomicilioJudicialPayloadValidator.PayloadInput(
                "12345678901", "sha256-abc", "INTIMACAO", true, true);
        var resultado = validator.validar(input);
        assertThat(resultado.valido()).isTrue();
        assertThat(resultado.erros()).isEmpty();
    }

    @Test
    void validar_invalido_quandoDestinatarioSemCadastro() {
        var input = new DomicilioJudicialPayloadValidator.PayloadInput(
                "12345678901", "sha256-abc", "INTIMACAO", false, true);
        var resultado = validator.validar(input);
        assertThat(resultado.valido()).isFalse();
        assertThat(resultado.erros()).anyMatch(e -> e.contains("não cadastrado"));
    }

    @Test
    void validar_invalido_quandoCpfCnpjAusente() {
        var input = new DomicilioJudicialPayloadValidator.PayloadInput(
                "", "sha256-abc", "INTIMACAO", true, true);
        var resultado = validator.validar(input);
        assertThat(resultado.valido()).isFalse();
        assertThat(resultado.erros()).anyMatch(e -> e.contains("CPF/CNPJ"));
    }

    @Test
    void validar_invalido_quandoAssinaturaInvalida() {
        var input = new DomicilioJudicialPayloadValidator.PayloadInput(
                "12345678901", "sha256-abc", "INTIMACAO", true, false);
        var resultado = validator.validar(input);
        assertThat(resultado.valido()).isFalse();
        assertThat(resultado.erros()).anyMatch(e -> e.contains("Assinatura digital"));
    }
}
