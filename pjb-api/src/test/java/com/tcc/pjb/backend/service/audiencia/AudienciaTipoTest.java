package com.tcc.pjb.backend.service.audiencia;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AudienciaTipoTest {

    @Test
    void admiteVirtual_custodiaNaoAdmite() {
        assertThat(AudienciaTipo.CUSTODIAL.admiteVirtual()).isFalse();
    }

    @Test
    void admiteVirtual_menoresNaoAdmite() {
        assertThat(AudienciaTipo.ESPECIAL_MENORES.admiteVirtual()).isFalse();
    }

    @Test
    void admiteVirtual_conciliacaoAdmite() {
        assertThat(AudienciaTipo.CONCILIACAO.admiteVirtual()).isTrue();
    }

    @Test
    void exigePresencaFisica_custodialEMenores() {
        assertThat(AudienciaTipo.CUSTODIAL.exigePresencaFisica()).isTrue();
        assertThat(AudienciaTipo.ESPECIAL_MENORES.exigePresencaFisica()).isTrue();
        assertThat(AudienciaTipo.INSTRUCAO_E_JULGAMENTO.exigePresencaFisica()).isFalse();
    }

    @Test
    void permiteGravacao_naoPermiteMenores() {
        assertThat(AudienciaTipo.ESPECIAL_MENORES.permiteGravacao()).isFalse();
        assertThat(AudienciaTipo.CONCILIACAO.permiteGravacao()).isTrue();
    }

    @Test
    void duracaoEstimada_conciliacaoSessenta() {
        assertThat(AudienciaTipo.CONCILIACAO.duracaoEstimadaMinutos()).isEqualTo(60);
    }

    @Test
    void duracaoEstimada_instrucao120() {
        assertThat(AudienciaTipo.INSTRUCAO_E_JULGAMENTO.duracaoEstimadaMinutos()).isEqualTo(120);
    }

    @Test
    void duracaoEstimada_sustentacaoOral20() {
        assertThat(AudienciaTipo.SUSTENTACAO_ORAL.duracaoEstimadaMinutos()).isEqualTo(20);
    }
}
