package com.tcc.pjb.backend.model.entity.servidor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.model.entity.enums.FuncaoServidorJudiciario;
import com.tcc.pjb.backend.model.entity.enums.StatusFuncaoServidorSolicitacao;
import org.junit.jupiter.api.Test;

class FuncaoServidorSolicitacaoTest {

    @Test
    void criadaComStatusPendenteERequestedAtPreenchido() {
        var solicitacao = new FuncaoServidorSolicitacao(10L, 5L, FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL, "Motivo");

        assertThat(solicitacao.getStatus()).isEqualTo(StatusFuncaoServidorSolicitacao.PENDENTE);
        assertThat(solicitacao.getRequestedAt()).isNotNull();
        assertThat(solicitacao.getSolicitanteId()).isEqualTo(10L);
        assertThat(solicitacao.getUnidadeId()).isEqualTo(5L);
        assertThat(solicitacao.getFuncao()).isEqualTo(FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL);
    }

    @Test
    void aprovarTransicionaParaAprovadaERegistraDecisor() {
        var solicitacao = new FuncaoServidorSolicitacao(10L, 5L, FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL, null);

        solicitacao.aprovar(99L);

        assertThat(solicitacao.getStatus()).isEqualTo(StatusFuncaoServidorSolicitacao.APROVADA);
        assertThat(solicitacao.getDecididoPorId()).isEqualTo(99L);
        assertThat(solicitacao.getDecididoEm()).isNotNull();
    }

    @Test
    void rejeitarTransicionaParaRejeitadaERegistraMotivo() {
        var solicitacao = new FuncaoServidorSolicitacao(10L, 5L, FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL, null);

        solicitacao.rejeitar(99L, "Unidade incorreta");

        assertThat(solicitacao.getStatus()).isEqualTo(StatusFuncaoServidorSolicitacao.REJEITADA);
        assertThat(solicitacao.getDecididoPorId()).isEqualTo(99L);
        assertThat(solicitacao.getMotivoRejeicao()).isEqualTo("Unidade incorreta");
    }

    @Test
    void aprovarSolicitacaoJaDecididaLancaExcecao() {
        var solicitacao = new FuncaoServidorSolicitacao(10L, 5L, FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL, null);
        solicitacao.aprovar(99L);

        assertThatThrownBy(() -> solicitacao.aprovar(1L)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejeitarSolicitacaoJaDecididaLancaExcecao() {
        var solicitacao = new FuncaoServidorSolicitacao(10L, 5L, FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL, null);
        solicitacao.rejeitar(99L, "motivo");

        assertThatThrownBy(() -> solicitacao.rejeitar(1L, "outro")).isInstanceOf(IllegalStateException.class);
    }
}
