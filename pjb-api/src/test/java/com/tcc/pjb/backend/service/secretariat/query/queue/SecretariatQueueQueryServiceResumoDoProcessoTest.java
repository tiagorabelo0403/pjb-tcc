package com.tcc.pjb.backend.service.secretariat.query.queue;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.Processo;
import org.junit.jupiter.api.Test;

class SecretariatQueueQueryServiceResumoDoProcessoTest {

    @Test
    void usaResumoIaQuandoPresente() {
        Processo processo = new Processo();
        processo.setResumoIA("Ação de indenização por danos morais decorrente de acidente de trânsito.");
        processo.setClasseProcessual("Procedimento Comum Cível");
        processo.setAssunto("Indenização");

        assertThat(SecretariatQueueQueryService.resumoDoProcesso(processo))
                .isEqualTo("Ação de indenização por danos morais decorrente de acidente de trânsito.");
    }

    @Test
    void caiParaClasseEAssuntoQuandoResumoIaAusente() {
        Processo processo = new Processo();
        processo.setClasseProcessual("Procedimento Comum Cível");
        processo.setAssunto("Indenização por Dano Moral");

        assertThat(SecretariatQueueQueryService.resumoDoProcesso(processo))
                .isEqualTo("Procedimento Comum Cível — Indenização por Dano Moral");
    }

    @Test
    void usaSoClasseQuandoAssuntoAusente() {
        Processo processo = new Processo();
        processo.setClasseProcessual("Procedimento Comum Cível");

        assertThat(SecretariatQueueQueryService.resumoDoProcesso(processo)).isEqualTo("Procedimento Comum Cível");
    }

    @Test
    void retornaNuloQuandoNenhumDadoDisponivel() {
        Processo processo = new Processo();

        assertThat(SecretariatQueueQueryService.resumoDoProcesso(processo)).isNull();
    }
}
