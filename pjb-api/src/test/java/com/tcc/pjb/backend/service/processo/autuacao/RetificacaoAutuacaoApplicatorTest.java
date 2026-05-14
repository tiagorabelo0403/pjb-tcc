package com.tcc.pjb.backend.service.processo.autuacao;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RetificacaoAutuacaoApplicatorTest {

    private final RetificacaoAutuacaoApplicator applicator =
            new RetificacaoAutuacaoApplicator(newPolicy());

    private static RetificacaoAutuacaoPolicy newPolicy() {
        try {
            var c = RetificacaoAutuacaoPolicy.class.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    @Test
    void aplicar_sucesso_quandoAprovadaComAprovacaoJudicial() {
        var input = new RetificacaoAutuacaoApplicator.AplicacaoInput(
                UUID.randomUUID(), UUID.randomUUID(),
                RetificacaoAutuacaoStatus.APROVADA, "servidor-001", true);
        var resultado = applicator.aplicar(input);
        assertThat(resultado.aplicada()).isTrue();
        assertThat(resultado.statusNovo()).isEqualTo(RetificacaoAutuacaoStatus.APLICADA);
    }

    @Test
    void aplicar_falha_quandoStatusNaoAprovada() {
        var input = new RetificacaoAutuacaoApplicator.AplicacaoInput(
                UUID.randomUUID(), UUID.randomUUID(),
                RetificacaoAutuacaoStatus.RASCUNHO, "servidor-001", true);
        var resultado = applicator.aplicar(input);
        assertThat(resultado.aplicada()).isFalse();
        assertThat(resultado.mensagem()).contains("APROVADA");
    }

    @Test
    void aplicar_falha_quandoStatusTerminal() {
        var input = new RetificacaoAutuacaoApplicator.AplicacaoInput(
                UUID.randomUUID(), UUID.randomUUID(),
                RetificacaoAutuacaoStatus.CANCELADA, "servidor-001", true);
        var resultado = applicator.aplicar(input);
        assertThat(resultado.aplicada()).isFalse();
        assertThat(resultado.mensagem()).contains("terminal");
    }

    @Test
    void aplicar_falha_quandoSemAprovacaoJudicial() {
        var input = new RetificacaoAutuacaoApplicator.AplicacaoInput(
                UUID.randomUUID(), UUID.randomUUID(),
                RetificacaoAutuacaoStatus.APROVADA, "servidor-001", false);
        var resultado = applicator.aplicar(input);
        assertThat(resultado.aplicada()).isFalse();
        assertThat(resultado.mensagem()).contains("aprovação judicial");
    }
}
