package com.tcc.pjb.backend.service.publicacao;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MeioPublicacaoTest {

    @Test
    void eEletronico_falso_paraEDITAL_FISICO() {
        assertThat(MeioPublicacao.EDITAL_FISICO.eEletronico()).isFalse();
    }

    @Test
    void eEletronico_verdadeiro_paraDIARIO_ELETRONICO() {
        assertThat(MeioPublicacao.DIARIO_ELETRONICO_JUSTICA.eEletronico()).isTrue();
    }

    @Test
    void requerIntimacaoPessoal_verdadeiro_paraEDITAL_FISICO() {
        assertThat(MeioPublicacao.EDITAL_FISICO.requerIntimacaoPessoal()).isTrue();
    }

    @Test
    void requerIntimacaoPessoal_falso_paraDIARIO_ELETRONICO() {
        assertThat(MeioPublicacao.DIARIO_ELETRONICO_JUSTICA.requerIntimacaoPessoal()).isFalse();
    }
}
