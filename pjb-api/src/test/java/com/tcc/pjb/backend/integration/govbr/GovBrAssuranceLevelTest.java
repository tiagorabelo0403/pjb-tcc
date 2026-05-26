package com.tcc.pjb.backend.integration.govbr;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GovBrAssuranceLevelTest {

    @Test
    void prata_minimosBronze_retornaTrue() {
        assertThat(GovBrAssuranceLevel.meetsMinimum(GovBrAssuranceLevel.PRATA, GovBrAssuranceLevel.BRONZE)).isTrue();
    }

    @Test
    void prata_minimosPrata_retornaTrue() {
        assertThat(GovBrAssuranceLevel.meetsMinimum(GovBrAssuranceLevel.PRATA, GovBrAssuranceLevel.PRATA)).isTrue();
    }

    @Test
    void prata_minimosOuro_retornaFalse() {
        assertThat(GovBrAssuranceLevel.meetsMinimum(GovBrAssuranceLevel.PRATA, GovBrAssuranceLevel.OURO)).isFalse();
    }

    @Test
    void ouro_minimosPrata_retornaTrue() {
        assertThat(GovBrAssuranceLevel.meetsMinimum(GovBrAssuranceLevel.OURO, GovBrAssuranceLevel.PRATA)).isTrue();
    }

    @Test
    void bronze_minimosPrata_retornaFalse() {
        assertThat(GovBrAssuranceLevel.meetsMinimum(GovBrAssuranceLevel.BRONZE, GovBrAssuranceLevel.PRATA)).isFalse();
    }

    @Test
    void nulo_retornaFalse() {
        assertThat(GovBrAssuranceLevel.meetsMinimum(null, GovBrAssuranceLevel.PRATA)).isFalse();
    }

    @Test
    void valorDesconhecido_retornaFalse() {
        assertThat(GovBrAssuranceLevel.meetsMinimum("https://example.com/loa/99", GovBrAssuranceLevel.PRATA)).isFalse();
    }
}
