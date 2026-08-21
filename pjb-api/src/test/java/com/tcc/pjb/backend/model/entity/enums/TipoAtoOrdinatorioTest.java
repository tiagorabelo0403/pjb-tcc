package com.tcc.pjb.backend.model.entity.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TipoAtoOrdinatorioTest {

    @Test
    void todosOsValoresTemLabelEFundamentoLegalNaoVazios() {
        for (TipoAtoOrdinatorio tipo : TipoAtoOrdinatorio.values()) {
            assertThat(tipo.label()).as("label de " + tipo).isNotBlank();
            assertThat(tipo.fundamentoLegal()).as("fundamentoLegal de " + tipo).isNotBlank();
            assertThat(tipo.fundamentoLegal()).as("fundamentoLegal de " + tipo + " cita CPC art. 203")
                    .contains("CPC art. 203");
        }
    }

    @Test
    void temSeisValoresCatalogados() {
        assertThat(TipoAtoOrdinatorio.values()).hasSize(6);
    }
}
