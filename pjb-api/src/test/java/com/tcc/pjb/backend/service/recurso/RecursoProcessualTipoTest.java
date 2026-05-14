package com.tcc.pjb.backend.service.recurso;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RecursoProcessualTipoTest {

    @Test
    void prazoBaseDias_embargosDeclaracao_retorna5() {
        assertThat(RecursoProcessualTipo.EMBARGOS_DECLARACAO.prazoBaseDias()).isEqualTo(5);
    }

    @Test
    void prazoBaseDias_recursoInominadoJec_retorna10() {
        assertThat(RecursoProcessualTipo.RECURSO_INOMINADO_JEC.prazoBaseDias()).isEqualTo(10);
    }

    @Test
    void prazoBaseDias_apelacao_retorna15() {
        assertThat(RecursoProcessualTipo.APELACAO.prazoBaseDias()).isEqualTo(15);
    }

    @Test
    void exigePreparo_embargosDeclaracao_false() {
        assertThat(RecursoProcessualTipo.EMBARGOS_DECLARACAO.exigePreparo()).isFalse();
    }

    @Test
    void exigePreparo_apelacao_true() {
        assertThat(RecursoProcessualTipo.APELACAO.exigePreparo()).isTrue();
    }

    @Test
    void admiteEfetoSuspensivo_apenasApelacao() {
        assertThat(RecursoProcessualTipo.APELACAO.admiteEfetoSuspensivo()).isTrue();
        assertThat(RecursoProcessualTipo.AGRAVO_DE_INSTRUMENTO.admiteEfetoSuspensivo()).isFalse();
    }

    @Test
    void ehExtraordinario_recursosExtraordinarios() {
        assertThat(RecursoProcessualTipo.RECURSO_ESPECIAL.ehExtraordinario()).isTrue();
        assertThat(RecursoProcessualTipo.RECURSO_EXTRAORDINARIO.ehExtraordinario()).isTrue();
        assertThat(RecursoProcessualTipo.RECURSO_ESPECIAL_REPETITIVO.ehExtraordinario()).isTrue();
        assertThat(RecursoProcessualTipo.RECURSO_EXTRAORDINARIO_REPERCUSSAO_GERAL.ehExtraordinario()).isTrue();
    }

    @Test
    void ehExtraordinario_apelacao_false() {
        assertThat(RecursoProcessualTipo.APELACAO.ehExtraordinario()).isFalse();
    }
}
