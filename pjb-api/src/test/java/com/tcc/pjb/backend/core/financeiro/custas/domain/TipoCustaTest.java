package com.tcc.pjb.backend.core.financeiro.custas.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TipoCustaTest {

    @Test
    void multasSaoIdentificadasComoMulta() {
        assertThat(TipoCusta.MULTA_LITIGANCIA_MA_FE.eMulta()).isTrue();
        assertThat(TipoCusta.MULTA_IMPROBIDADE_ART77.eMulta()).isTrue();
        assertThat(TipoCusta.MULTA_ART_1026_CPC.eMulta()).isTrue();
    }

    @Test
    void naoMultasNaoSaoIdentificadasComoMulta() {
        assertThat(TipoCusta.CUSTAS_INICIAIS.eMulta()).isFalse();
        assertThat(TipoCusta.PREPARO_RECURSAL.eMulta()).isFalse();
        assertThat(TipoCusta.PERITO_ADIANTAMENTO.eMulta()).isFalse();
        assertThat(TipoCusta.HONORARIOS_PERICIAIS.eMulta()).isFalse();
        assertThat(TipoCusta.EMOLUMENTO_CARTORIAL.eMulta()).isFalse();
        assertThat(TipoCusta.CUSTAS_EXPEDICAO_MANDADO.eMulta()).isFalse();
    }

    @Test
    void multasSubjetivasRequeremDespacho() {
        assertThat(TipoCusta.MULTA_LITIGANCIA_MA_FE.requerDespacho()).isTrue();
        assertThat(TipoCusta.MULTA_IMPROBIDADE_ART77.requerDespacho()).isTrue();
    }

    @Test
    void multaObjetivaDoArt1026NaoRequerDespacho() {
        assertThat(TipoCusta.MULTA_ART_1026_CPC.requerDespacho()).isFalse();
    }

    @Test
    void apenasCustasIniciaisAplicaAoAjuizamentoInicial() {
        assertThat(TipoCusta.CUSTAS_INICIAIS.aplicaAoAjuizamentoInicial()).isTrue();
        assertThat(TipoCusta.PREPARO_RECURSAL.aplicaAoAjuizamentoInicial()).isFalse();
        assertThat(TipoCusta.MULTA_LITIGANCIA_MA_FE.aplicaAoAjuizamentoInicial()).isFalse();
    }

    @Test
    void apenasPreparoRecursalAplicaAoRecursal() {
        assertThat(TipoCusta.PREPARO_RECURSAL.aplicaAoRecursal()).isTrue();
        assertThat(TipoCusta.CUSTAS_INICIAIS.aplicaAoRecursal()).isFalse();
        assertThat(TipoCusta.HONORARIOS_PERICIAIS.aplicaAoRecursal()).isFalse();
    }

    @Test
    void fundamentoLegalCitaBaseNormativaCorretaPorTipo() {
        assertThat(TipoCusta.CUSTAS_INICIAIS.fundamentoLegal()).isEqualTo("CPC, art. 82");
        assertThat(TipoCusta.PREPARO_RECURSAL.fundamentoLegal()).isEqualTo("CPC, art. 1.007");
        assertThat(TipoCusta.PERITO_ADIANTAMENTO.fundamentoLegal()).isEqualTo("CPC, art. 95");
        assertThat(TipoCusta.HONORARIOS_PERICIAIS.fundamentoLegal()).isEqualTo("CPC, art. 465");
        assertThat(TipoCusta.MULTA_LITIGANCIA_MA_FE.fundamentoLegal()).isEqualTo("CPC, art. 81");
        assertThat(TipoCusta.MULTA_IMPROBIDADE_ART77.fundamentoLegal()).isEqualTo("CPC, art. 77, § 2º");
        assertThat(TipoCusta.MULTA_ART_1026_CPC.fundamentoLegal()).isEqualTo("CPC, art. 1.026, § 2º");
        assertThat(TipoCusta.EMOLUMENTO_CARTORIAL.fundamentoLegal()).isEqualTo("Lei 10.169/2000");
        assertThat(TipoCusta.CUSTAS_EXPEDICAO_MANDADO.fundamentoLegal()).isEqualTo("CPC, art. 82, § 1º");
    }

    @Test
    void fundamentoLegalCobreTodosOsValoresDoEnum() {
        for (TipoCusta tipo : TipoCusta.values()) {
            assertThat(tipo.fundamentoLegal()).as("fundamento legal do tipo %s", tipo).isNotBlank();
        }
    }
}
