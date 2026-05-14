package com.tcc.pjb.backend.service.processo.autuacao;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RetificacaoAutuacaoPolicyTest {

    private final RetificacaoAutuacaoPolicy policy = newPolicy();

    private static RetificacaoAutuacaoPolicy newPolicy() {
        try {
            var c = RetificacaoAutuacaoPolicy.class.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    @Test
    void avaliar_permitida_quandoSemImpedimentos() {
        var input = new RetificacaoAutuacaoPolicy.RetificacaoInput(
                RitoProcessual.PROCEDIMENTO_COMUM, RitoProcessual.PROCEDIMENTO_COMUM,
                "Acao Civil", "Acao Civil Publica",
                false, false, "Erro material na autuacao");
        var decisao = policy.avaliar(input);
        assertThat(decisao.permitida()).isTrue();
        assertThat(decisao.impedimentos()).isEmpty();
    }

    @Test
    void avaliar_bloqueada_quandoProcessoComSentenca() {
        var input = new RetificacaoAutuacaoPolicy.RetificacaoInput(
                RitoProcessual.PROCEDIMENTO_COMUM, RitoProcessual.PROCEDIMENTO_COMUM,
                "Acao Civil", "Acao Civil Publica",
                false, true, "Correcao");
        var decisao = policy.avaliar(input);
        assertThat(decisao.permitida()).isFalse();
        assertThat(decisao.impedimentos()).anyMatch(i -> i.contains("sentença transitada"));
    }

    @Test
    void avaliar_alertaRito_quandoMudancaDeRito() {
        var input = new RetificacaoAutuacaoPolicy.RetificacaoInput(
                RitoProcessual.PROCEDIMENTO_COMUM, RitoProcessual.JUIZADO_ESPECIAL_CIVEL,
                "Acao", "Acao",
                false, false, "Correcao de rito");
        var decisao = policy.avaliar(input);
        assertThat(decisao.alertas()).anyMatch(a -> a.contains("Alteração de rito"));
    }

    @Test
    void avaliar_bloqueada_quandoCitadoEMudancaDeRito() {
        var input = new RetificacaoAutuacaoPolicy.RetificacaoInput(
                RitoProcessual.PROCEDIMENTO_COMUM, RitoProcessual.JUIZADO_ESPECIAL_CIVEL,
                "Acao", "Acao",
                true, false, "Correcao");
        var decisao = policy.avaliar(input);
        assertThat(decisao.permitida()).isFalse();
        assertThat(decisao.impedimentos()).anyMatch(i -> i.contains("já citada"));
    }

    @Test
    void avaliar_bloqueada_quandoMotivoAusente() {
        var input = new RetificacaoAutuacaoPolicy.RetificacaoInput(
                RitoProcessual.PROCEDIMENTO_COMUM, RitoProcessual.PROCEDIMENTO_COMUM,
                "Acao", "Acao",
                false, false, "");
        var decisao = policy.avaliar(input);
        assertThat(decisao.permitida()).isFalse();
        assertThat(decisao.impedimentos()).anyMatch(i -> i.contains("Motivo da retificação obrigatório"));
    }
}
