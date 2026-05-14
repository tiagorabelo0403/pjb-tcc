package com.tcc.pjb.backend.service.communication.domicilio;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DomicilioJudicialFailureClassifierTest {

    private final DomicilioJudicialFailureClassifier classifier = newClassifier();

    private static DomicilioJudicialFailureClassifier newClassifier() {
        try {
            var c = DomicilioJudicialFailureClassifier.class.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    @Test
    void classificar_indisponibilidadeServico_quandoHttp503() {
        var falha = classifier.classificar(503, null);
        assertThat(falha.classificacao())
                .isEqualTo(DomicilioJudicialFailureClassifier.FalhaClassificacao.INDISPONIBILIDADE_SERVICO);
        assertThat(falha.recomendaRetry()).isTrue();
    }

    @Test
    void classificar_credencialInvalida_quandoHttp401() {
        var falha = classifier.classificar(401, null);
        assertThat(falha.classificacao())
                .isEqualTo(DomicilioJudicialFailureClassifier.FalhaClassificacao.CREDENCIAL_INVALIDA);
        assertThat(falha.recomendaRetry()).isFalse();
    }

    @Test
    void classificar_destinatarioNaoEncontrado_quandoHttp404() {
        var falha = classifier.classificar(404, null);
        assertThat(falha.classificacao())
                .isEqualTo(DomicilioJudicialFailureClassifier.FalhaClassificacao.DESTINATARIO_NAO_ENCONTRADO);
        assertThat(falha.recomendaRetry()).isFalse();
    }

    @Test
    void classificar_caixaCheia_quandoMensagemContemCaixaCheia() {
        var falha = classifier.classificar(200, "caixa cheia");
        assertThat(falha.classificacao())
                .isEqualTo(DomicilioJudicialFailureClassifier.FalhaClassificacao.CAIXA_CHEIA);
        assertThat(falha.recomendaRetry()).isTrue();
    }

    @Test
    void classificar_transitoria_quandoHttp500() {
        var falha = classifier.classificar(500, null);
        assertThat(falha.classificacao())
                .isEqualTo(DomicilioJudicialFailureClassifier.FalhaClassificacao.TRANSITORIA);
        assertThat(falha.recomendaRetry()).isTrue();
    }

    @Test
    void classificar_desconhecida_quandoStatusNaoMapeado() {
        var falha = classifier.classificar(200, "erro inesperado");
        assertThat(falha.classificacao())
                .isEqualTo(DomicilioJudicialFailureClassifier.FalhaClassificacao.DESCONHECIDA);
        assertThat(falha.recomendaRetry()).isFalse();
    }
}
