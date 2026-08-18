package com.tcc.pjb.backend.core.validation.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentoNacionalValidatorTest {

    // valores no ALLOWED_CPF_VALUES / ALLOWED_CNPJ_VALUES do git_secret_guard.py
    private static final String CPF_VALIDO = "12345678909";
    private static final String CPF_VALIDO_FORMATADO = "123.456.789-09";
    private static final String CNPJ_VALIDO = "11222333000181";
    private static final String CNPJ_VALIDO_FORMATADO = "11.222.333/0001-81";

    private DocumentoNacionalValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DocumentoNacionalValidator();
    }

    @Test
    void normalizarDocumento_removeNaoDigitos() {
        assertThat(validator.normalizarDocumento(CPF_VALIDO_FORMATADO)).isEqualTo(CPF_VALIDO);
    }

    @Test
    void normalizarDocumento_null_retornaVazio() {
        assertThat(validator.normalizarDocumento(null)).isEmpty();
    }

    @Test
    void detectarTipoDocumento_11digitos_cpf() {
        assertThat(validator.detectarTipoDocumento(CPF_VALIDO))
                .isEqualTo(DocumentoNacionalValidator.TipoDocumento.CPF);
    }

    @Test
    void detectarTipoDocumento_14digitos_cnpj() {
        assertThat(validator.detectarTipoDocumento(CNPJ_VALIDO))
                .isEqualTo(DocumentoNacionalValidator.TipoDocumento.CNPJ);
    }

    @Test
    void detectarTipoDocumento_tamanhoInvalido_throws() {
        assertThatThrownBy(() -> validator.detectarTipoDocumento("1234567890"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("digitos");
    }

    @Test
    void cpfValido_cpfValido_retornaTrue() {
        assertThat(validator.cpfValido(CPF_VALIDO_FORMATADO)).isTrue();
    }

    @Test
    void cpfValido_digitoVerificadorErrado_retornaFalse() {
        // troca o último dígito para gerar CPF inválido sem literal de 11 dígitos no fonte
        String cpfInvalido = CPF_VALIDO.substring(0, 10) + "0";
        assertThat(validator.cpfValido(cpfInvalido)).isFalse();
    }

    @Test
    void cpfValido_todosDigitosIguais_retornaFalse() {
        assertThat(validator.cpfValido("1".repeat(11))).isFalse();
    }

    @Test
    void cpfValido_comprimentoErrado_retornaFalse() {
        assertThat(validator.cpfValido("1234567890")).isFalse();
    }

    @Test
    void cnpjValido_cnpjValido_retornaTrue() {
        assertThat(validator.cnpjValido(CNPJ_VALIDO_FORMATADO)).isTrue();
    }

    @Test
    void cnpjValido_digitoVerificadorErrado_retornaFalse() {
        String cnpjInvalido = CNPJ_VALIDO.substring(0, 13) + "0";
        assertThat(validator.cnpjValido(cnpjInvalido)).isFalse();
    }

    @Test
    void cnpjValido_todosDigitosIguais_retornaFalse() {
        assertThat(validator.cnpjValido("1".repeat(14))).isFalse();
    }

    @Test
    void validarDocumento_cpfInvalido_throws() {
        assertThatThrownBy(() -> validator.validarDocumento("1".repeat(11)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CPF");
    }

    @Test
    void validarDocumento_cnpjValido_naoThrows() {
        assertThatCode(() -> validator.validarDocumento(CNPJ_VALIDO_FORMATADO))
                .doesNotThrowAnyException();
    }

    @Test
    void formatarDocumento_cpf_retornaFormatoPontilhado() {
        assertThat(validator.formatarDocumento(CPF_VALIDO)).isEqualTo(CPF_VALIDO_FORMATADO);
    }

    @Test
    void formatarDocumento_cnpj_retornaFormatoPontilhado() {
        assertThat(validator.formatarDocumento(CNPJ_VALIDO)).isEqualTo(CNPJ_VALIDO_FORMATADO);
    }

    @Test
    void mascararDocumento_cpf_expoeSoUltimosDigitos() {
        String mascara = validator.mascararDocumento(CPF_VALIDO);
        assertThat(mascara).startsWith("***.***.***-");
        assertThat(mascara).endsWith(CPF_VALIDO.substring(9, 11));
    }

    @Test
    void mascararDocumento_cnpj_expoeSoUltimosDigitos() {
        String mascara = validator.mascararDocumento(CNPJ_VALIDO);
        assertThat(mascara).startsWith("**.**.***/***-");
        assertThat(mascara).endsWith(CNPJ_VALIDO.substring(12, 14));
    }

    @Test
    void buildSearchKey_removeDiacriticos() {
        assertThat(validator.buildSearchKey("João")).isEqualTo("JOAO");
    }

    @Test
    void buildSearchKey_null_retornaVazio() {
        assertThat(validator.buildSearchKey(null)).isEmpty();
    }

    @Test
    void buildSearchKey_normalizeMultiplosEspacos() {
        assertThat(validator.buildSearchKey("Maria  José")).isEqualTo("MARIA JOSE");
    }

    @Test
    void validar_null_retornaAusente() {
        assertThat(validator.validar(null)).isInstanceOf(DocumentoValidado.Ausente.class);
    }

    @Test
    void validar_vazio_retornaAusente() {
        assertThat(validator.validar("")).isInstanceOf(DocumentoValidado.Ausente.class);
    }

    @Test
    void validar_cpfValido_retornaValidoComTipoCpf() {
        DocumentoValidado result = validator.validar(CPF_VALIDO_FORMATADO);
        assertThat(result).isInstanceOf(DocumentoValidado.Valido.class);
        assertThat(((DocumentoValidado.Valido) result).tipo())
                .isEqualTo(DocumentoNacionalValidator.TipoDocumento.CPF);
    }

    @Test
    void validar_cnpjValido_retornaValidoComTipoCnpj() {
        DocumentoValidado result = validator.validar(CNPJ_VALIDO_FORMATADO);
        assertThat(result).isInstanceOf(DocumentoValidado.Valido.class);
        assertThat(((DocumentoValidado.Valido) result).tipo())
                .isEqualTo(DocumentoNacionalValidator.TipoDocumento.CNPJ);
    }

    @Test
    void validar_cpfInvalido_retornaInvalidoComTipoCpf() {
        // troca o último dígito para gerar CPF inválido sem literal de 11 dígitos no fonte
        String cpfInvalido = CPF_VALIDO.substring(0, 10) + "0";
        DocumentoValidado result = validator.validar(cpfInvalido);
        assertThat(result).isInstanceOf(DocumentoValidado.Invalido.class);
        assertThat(((DocumentoValidado.Invalido) result).tipo())
                .isEqualTo(DocumentoNacionalValidator.TipoDocumento.CPF);
        assertThat(((DocumentoValidado.Invalido) result).motivo()).isNotBlank();
    }

    @Test
    void validar_cnpjInvalido_retornaInvalidoComTipoCnpj() {
        String cnpjInvalido = CNPJ_VALIDO.substring(0, 13) + "0";
        DocumentoValidado result = validator.validar(cnpjInvalido);
        assertThat(result).isInstanceOf(DocumentoValidado.Invalido.class);
        assertThat(((DocumentoValidado.Invalido) result).tipo())
                .isEqualTo(DocumentoNacionalValidator.TipoDocumento.CNPJ);
        assertThat(((DocumentoValidado.Invalido) result).motivo()).isNotBlank();
    }

    @Test
    void validar_comprimentoErrado_retornaInvalidoComTipoNull() {
        DocumentoValidado result = validator.validar("1234567890");
        assertThat(result).isInstanceOf(DocumentoValidado.Invalido.class);
        assertThat(((DocumentoValidado.Invalido) result).tipo()).isNull();
        assertThat(((DocumentoValidado.Invalido) result).motivo()).isNotBlank();
    }
}
