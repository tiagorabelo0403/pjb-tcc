package com.tcc.pjb.backend.service.prazo;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PrazoTipoTest {

    @Test
    void eFatal_verdadeiro_paraPEREMPTORIO() {
        assertThat(PrazoTipo.PEREMPTÓRIO.eFatal()).isTrue();
    }

    @Test
    void eFatal_verdadeiro_paraLEGAL() {
        assertThat(PrazoTipo.LEGAL.eFatal()).isTrue();
    }

    @Test
    void eFatal_falso_paraDILATORIO() {
        assertThat(PrazoTipo.DILATÓRIO.eFatal()).isFalse();
    }

    @Test
    void admiteProrogacao_verdadeiro_paraDILATORIO() {
        assertThat(PrazoTipo.DILATÓRIO.admiteProrogacao()).isTrue();
    }

    @Test
    void admiteProrogacao_verdadeiro_paraJUDICIAL() {
        assertThat(PrazoTipo.JUDICIAL.admiteProrogacao()).isTrue();
    }

    @Test
    void admiteProrogacao_falso_paraPEREMPTORIO() {
        assertThat(PrazoTipo.PEREMPTÓRIO.admiteProrogacao()).isFalse();
    }
}
