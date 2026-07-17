package com.tcc.pjb.backend.model.dto.processual;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AncoraTerritorialTest {

    @Test
    void ibgeValidoDe7DigitosEhResolvivel() {
        AncoraTerritorial ancora = new AncoraTerritorial("2307304", "Morada Nova", "CE");

        assertThat(ancora.resolvivel()).isTrue();
        assertThat(ancora.municipioIbge()).isEqualTo("2307304");
    }

    @Test
    void ibgeMalformadoNormalizaParaNuloENaoResolvivel() {
        assertThat(new AncoraTerritorial("123", "Morada Nova", "CE").resolvivel()).isFalse();
        assertThat(new AncoraTerritorial("ABCDEFG", "Morada Nova", "CE").resolvivel()).isFalse();
        assertThat(new AncoraTerritorial("12345678", "Morada Nova", "CE").resolvivel()).isFalse();
    }

    @Test
    void ufMinusculaEhNormalizadaParaMaiuscula() {
        AncoraTerritorial ancora = new AncoraTerritorial(null, "Fortaleza", "ce");

        assertThat(ancora.uf()).isEqualTo("CE");
    }

    @Test
    void camposEmBrancoOuWhitespaceNormalizamParaNulo() {
        AncoraTerritorial ancora = new AncoraTerritorial("   ", "  ", "  ");

        assertThat(ancora.municipioIbge()).isNull();
        assertThat(ancora.municipio()).isNull();
        assertThat(ancora.uf()).isNull();
    }
}
