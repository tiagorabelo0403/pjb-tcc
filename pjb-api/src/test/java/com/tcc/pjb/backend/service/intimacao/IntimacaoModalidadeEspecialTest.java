package com.tcc.pjb.backend.service.intimacao;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class IntimacaoModalidadeEspecialTest {

    @Test
    void exigeOficialJustica_apenasOfficialJustica() {
        assertThat(IntimacaoModalidadeEspecial.PESSOAL_OFICIAL_JUSTICA.exigeOficialJustica()).isTrue();
        assertThat(IntimacaoModalidadeEspecial.PESSOAL_VIA_CORREIO_AR.exigeOficialJustica()).isFalse();
    }

    @Test
    void ehEletronica_somentesEletronicas() {
        assertThat(IntimacaoModalidadeEspecial.ELETRONICA_DOMICILIO_JUDICIAL.ehEletronica()).isTrue();
        assertThat(IntimacaoModalidadeEspecial.ELETRONICA_EMAIL_CADASTRADO.ehEletronica()).isTrue();
        assertThat(IntimacaoModalidadeEspecial.PESSOAL_OFICIAL_JUSTICA.ehEletronica()).isFalse();
    }

    @Test
    void ehEditalicia() {
        assertThat(IntimacaoModalidadeEspecial.EDITALICIA.ehEditalicia()).isTrue();
        assertThat(IntimacaoModalidadeEspecial.PESSOAL_VIA_CORREIO_AR.ehEditalicia()).isFalse();
    }

    @Test
    void prazoContagem_editalicia20() {
        assertThat(IntimacaoModalidadeEspecial.EDITALICIA.prazoContagem()).isEqualTo(20);
    }

    @Test
    void prazoContagem_cartaPrecatoria15() {
        assertThat(IntimacaoModalidadeEspecial.CARTA_PRECATORIA.prazoContagem()).isEqualTo(15);
    }

    @Test
    void prazoContagem_padrao5() {
        assertThat(IntimacaoModalidadeEspecial.PESSOAL_OFICIAL_JUSTICA.prazoContagem()).isEqualTo(5);
    }

    @Test
    void naoAdmiteSubstituicao_orgaosEspeciais() {
        assertThat(IntimacaoModalidadeEspecial.PESSOAL_DEFENSOR_PUBLICO
                .admiteSubstituicaoPorElectronica()).isFalse();
        assertThat(IntimacaoModalidadeEspecial.PESSOAL_MINISTERIO_PUBLICO
                .admiteSubstituicaoPorElectronica()).isFalse();
        assertThat(IntimacaoModalidadeEspecial.PESSOAL_FAZENDA_PUBLICA
                .admiteSubstituicaoPorElectronica()).isFalse();
    }
}
