package com.tcc.pjb.backend.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import org.junit.jupiter.api.Test;

class NumeroProcessoTest {

    @Test
    void deveGerarNumeroNoFormatoCnjComDigitoVerificadorCorreto() {
        NumeroProcesso numero = NumeroProcesso.gerarCnj(1, 2026, 8, 6, 1);

        assertThat(numero.getValor()).isEqualTo("0000001-32.2026.8.06.0001");
        assertThat(NumeroProcesso.validar(numero.getValor())).isTrue();
    }

    @Test
    void deveRejeitarNumeroCnjComDigitoVerificadorIncorreto() {
        assertThat(NumeroProcesso.validar("0000001-31.2026.8.06.0001")).isFalse();
    }

    @Test
    void deveGerarNumerosDistintosParaSequenciaisDistintos() {
        NumeroProcesso primeiro = NumeroProcesso.gerarCnj(1, 2026, 8, 6, 1);
        NumeroProcesso segundo = NumeroProcesso.gerarCnj(2, 2026, 8, 6, 1);

        assertThat(primeiro.getValor()).isNotEqualTo(segundo.getValor());
        assertThat(NumeroProcesso.validar(segundo.getValor())).isTrue();
    }

    @Test
    void deveExigirFormatoCnjCompletoNaFactory() {
        assertThatThrownBy(() -> NumeroProcesso.of("LD1-2026"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveUsarSegmentoCnjRealParaJusticaEstadual() {
        assertThat(TipoJustica.ESTADUAL.getCodigoCNJ()).isEqualTo("8");
    }
}
