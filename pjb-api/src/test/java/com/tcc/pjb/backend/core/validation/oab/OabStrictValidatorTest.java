package com.tcc.pjb.backend.core.validation.oab;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OabStrictValidatorTest {

    private OabStrictValidator validator;

    @BeforeEach
    void setUp() {
        validator = new OabStrictValidator();
    }

    @Test
    void parseAndValidate_formatoPadrao_retornaOabInfo() {
        OabInfo info = validator.parseAndValidate("SP-123456");
        assertThat(info.uf()).isEqualTo("SP");
        assertThat(info.numero()).isEqualTo("123456");
        assertThat(info.sufixo()).isNull();
    }

    @Test
    void parseAndValidate_formatoInvertidoNumeroUF() {
        OabInfo info = validator.parseAndValidate("123456-SP");
        assertThat(info.uf()).isEqualTo("SP");
        assertThat(info.numero()).isEqualTo("123456");
    }

    @Test
    void parseAndValidate_prefixoOabBarra() {
        OabInfo info = validator.parseAndValidate("OAB/SP 123456");
        assertThat(info.uf()).isEqualTo("SP");
        assertThat(info.numero()).isEqualTo("123456");
    }

    @Test
    void parseAndValidate_comPontos_removePontos() {
        OabInfo info = validator.parseAndValidate("SP-123.456");
        assertThat(info.numero()).isEqualTo("123456");
    }

    @Test
    void parseAndValidate_zerosLideres_removidos() {
        OabInfo info = validator.parseAndValidate("SP-0012345");
        assertThat(info.numero()).isEqualTo("12345");
    }

    @Test
    void parseAndValidate_comSufixo_retornaSufixo() {
        OabInfo info = validator.parseAndValidate("SP-123456-A");
        assertThat(info.sufixo()).isEqualTo("A");
    }

    @Test
    void parseAndValidate_displayFormatadoCorreto() {
        OabInfo info = validator.parseAndValidate("SP-123456");
        assertThat(info.canonicalDisplay()).isEqualTo("OAB/SP 123456");
    }

    @Test
    void parseAndValidate_normalizedFormatoCanônico() {
        OabInfo info = validator.parseAndValidate("SP-123456");
        assertThat(info.normalized()).isEqualTo("SP-123456");
    }

    @Test
    void parseAndValidate_null_throws() {
        assertThatThrownBy(() -> validator.parseAndValidate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vazio");
    }

    @Test
    void parseAndValidate_vazio_throws() {
        assertThatThrownBy(() -> validator.parseAndValidate("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vazio");
    }

    @Test
    void parseAndValidate_ufInvalida_throws() {
        assertThatThrownBy(() -> validator.parseAndValidate("XX-123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UF");
    }

    @Test
    void parseAndValidate_formatoNaoReconhecido_throws() {
        assertThatThrownBy(() -> validator.parseAndValidate("INVALIDO_FORMATO"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseAndValidate_todasUFsValidas() {
        assertThatCode(() -> validator.parseAndValidate("RJ-999999")).doesNotThrowAnyException();
        assertThatCode(() -> validator.parseAndValidate("DF-100")).doesNotThrowAnyException();
        assertThatCode(() -> validator.parseAndValidate("AM-5000")).doesNotThrowAnyException();
    }

    @Test
    void parseAndValidate_displayComSufixo() {
        OabInfo info = validator.parseAndValidate("SP-123456-B");
        assertThat(info.canonicalDisplay()).isEqualTo("OAB/SP 123456-B");
        assertThat(info.normalized()).isEqualTo("SP-123456-B");
    }
}
