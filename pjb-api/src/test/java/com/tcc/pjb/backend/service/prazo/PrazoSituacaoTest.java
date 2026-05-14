package com.tcc.pjb.backend.service.prazo;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PrazoSituacaoTest {

    @Test
    void eAtivo_verdadeiro_paraABERTO() {
        assertThat(PrazoSituacao.ABERTO.eAtivo()).isTrue();
    }

    @Test
    void eAtivo_verdadeiro_paraPRORROGADO() {
        assertThat(PrazoSituacao.PRORROGADO.eAtivo()).isTrue();
    }

    @Test
    void eAtivo_falso_paraENCERRADO() {
        assertThat(PrazoSituacao.ENCERRADO.eAtivo()).isFalse();
    }

    @Test
    void eTerminado_verdadeiro_paraENCERRADO() {
        assertThat(PrazoSituacao.ENCERRADO.eTerminado()).isTrue();
    }

    @Test
    void eTerminado_verdadeiro_paraDECURSO_CERTIFICADO() {
        assertThat(PrazoSituacao.DECURSO_CERTIFICADO.eTerminado()).isTrue();
    }

    @Test
    void eTerminado_falso_paraSUSPENSO() {
        assertThat(PrazoSituacao.SUSPENSO.eTerminado()).isFalse();
    }
}
