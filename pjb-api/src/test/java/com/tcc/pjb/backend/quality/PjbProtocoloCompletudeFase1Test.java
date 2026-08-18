package com.tcc.pjb.backend.quality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.core.protocolo.completude.ProtocoloCompletudeStateMachine;
import com.tcc.pjb.backend.core.protocolo.completude.domain.FundamentoNormativo;
import com.tcc.pjb.backend.core.protocolo.completude.domain.ResultadoValidacao;
import com.tcc.pjb.backend.core.protocolo.completude.domain.ViolacaoCompletude;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.FonteNormativaTipo;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.GrauExigibilidade;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.ProtocoloCompletudeStatus;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.SeveridadeCompletude;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbProtocoloCompletudeFase1Test {

    @Test
    void state_machine_aceita_transicoes_validas() {
        ProtocoloCompletudeStateMachine.validar(
                ProtocoloCompletudeStatus.RECEBIDO, ProtocoloCompletudeStatus.EM_VALIDACAO);
        ProtocoloCompletudeStateMachine.validar(
                ProtocoloCompletudeStatus.EM_VALIDACAO, ProtocoloCompletudeStatus.PENDENTE_DOCUMENTACAO);
        ProtocoloCompletudeStateMachine.validar(
                ProtocoloCompletudeStatus.EM_VALIDACAO, ProtocoloCompletudeStatus.COMPLETO);
        ProtocoloCompletudeStateMachine.validar(
                ProtocoloCompletudeStatus.PENDENTE_DOCUMENTACAO, ProtocoloCompletudeStatus.EM_VALIDACAO);
        ProtocoloCompletudeStateMachine.validar(
                ProtocoloCompletudeStatus.PENDENTE_DOCUMENTACAO, ProtocoloCompletudeStatus.DISPENSADO);
        ProtocoloCompletudeStateMachine.validar(
                ProtocoloCompletudeStatus.COMPLETO, ProtocoloCompletudeStatus.DISTRIBUIDO);
    }

    @Test
    void state_machine_rejeita_transicao_invalida() {
        assertThatThrownBy(() ->
                ProtocoloCompletudeStateMachine.validar(
                        ProtocoloCompletudeStatus.COMPLETO, ProtocoloCompletudeStatus.PENDENTE_DOCUMENTACAO))
                .isInstanceOf(ProtocoloCompletudeStateMachine.TransicaoInvalidaException.class)
                .hasMessageContaining("COMPLETO")
                .hasMessageContaining("PENDENTE_DOCUMENTACAO");
    }

    @Test
    void state_machine_rejeita_transicao_de_distribuido() {
        assertThatThrownBy(() ->
                ProtocoloCompletudeStateMachine.validar(
                        ProtocoloCompletudeStatus.DISTRIBUIDO, ProtocoloCompletudeStatus.RECEBIDO))
                .isInstanceOf(ProtocoloCompletudeStateMachine.TransicaoInvalidaException.class);
    }

    @Test
    void state_machine_rejeita_transicao_de_cancelado() {
        assertThatThrownBy(() ->
                ProtocoloCompletudeStateMachine.validar(
                        ProtocoloCompletudeStatus.CANCELADO, ProtocoloCompletudeStatus.EM_VALIDACAO))
                .isInstanceOf(ProtocoloCompletudeStateMachine.TransicaoInvalidaException.class);
    }

    @Test
    void transicao_valida_retorna_true() {
        assertThat(ProtocoloCompletudeStateMachine.transicaoValida(
                ProtocoloCompletudeStatus.EM_VALIDACAO, ProtocoloCompletudeStatus.COMPLETO)).isTrue();
    }

    @Test
    void transicao_invalida_retorna_false() {
        assertThat(ProtocoloCompletudeStateMachine.transicaoValida(
                ProtocoloCompletudeStatus.DISTRIBUIDO, ProtocoloCompletudeStatus.RECEBIDO)).isFalse();
    }

    @Test
    void resultado_validacao_detecta_bloqueante() {
        FundamentoNormativo fundamento = new FundamentoNormativo(
                FonteNormativaTipo.LEI, "CPC art. 319, II",
                "Qualificação das partes obrigatória", GrauExigibilidade.ABSOLUTO, "CPC");
        ViolacaoCompletude violacao = new ViolacaoCompletude.DocumentoObrigatorioAusente(
                TipoDocumento.DOCUMENTO_IDENTIDADE, fundamento);

        ResultadoValidacao resultado = new ResultadoValidacao(
                ProtocoloCompletudeStatus.PENDENTE_DOCUMENTACAO, List.of(violacao), "v1.0");

        assertThat(resultado.temBloqueante()).isTrue();
        assertThat(resultado.bloqueantes()).hasSize(1);
        assertThat(resultado.advertencias()).isEmpty();
        assertThat(resultado.statusResultante()).isEqualTo(ProtocoloCompletudeStatus.PENDENTE_DOCUMENTACAO);
    }

    @Test
    void resultado_validacao_sem_bloqueante_nao_bloqueia() {
        FundamentoNormativo fundamento = new FundamentoNormativo(
                FonteNormativaTipo.REGRA_INTERNA, null,
                "Qualidade de digitalização recomendada", GrauExigibilidade.DISPENSAVEL_COM_JUSTIFICATIVA, null);
        ViolacaoCompletude advertencia = new ViolacaoCompletude.QualidadeDigitalizacaoBaixa(
                TipoDocumento.DOCUMENTO_IDENTIDADE, 0.45, fundamento);

        ResultadoValidacao resultado = new ResultadoValidacao(
                ProtocoloCompletudeStatus.COMPLETO, List.of(advertencia), "v1.0");

        assertThat(resultado.temBloqueante()).isFalse();
        assertThat(resultado.bloqueantes()).isEmpty();
        assertThat(resultado.advertencias()).hasSize(1);
    }

    @Test
    void violacao_documento_ausente_tem_codigo_e_severidade_corretos() {
        FundamentoNormativo fundamento = new FundamentoNormativo(
                FonteNormativaTipo.LEI, "CPC art. 287", "Procuração obrigatória",
                GrauExigibilidade.DISPENSAVEL_COM_JUSTIFICATIVA, "CPC");
        ViolacaoCompletude violacao = new ViolacaoCompletude.DocumentoObrigatorioAusente(
                TipoDocumento.PROCURACAO, fundamento);

        assertThat(violacao.codigo()).isEqualTo("DOC_OBRIGATORIO_AUSENTE");
        assertThat(violacao.severidade()).isEqualTo(SeveridadeCompletude.BLOQUEANTE);
        assertThat(violacao.campo()).isEqualTo("PROCURACAO");
        assertThat(violacao.fundamento()).isEqualTo(fundamento);
    }

    @Test
    void fundamento_normativo_preserva_grau_exigibilidade() {
        FundamentoNormativo fundamento = new FundamentoNormativo(
                FonteNormativaTipo.LEI, "CLT art. 41",
                "CTPS obrigatória", GrauExigibilidade.RELATIVO, "TST");

        assertThat(fundamento.grau()).isEqualTo(GrauExigibilidade.RELATIVO);
        assertThat(fundamento.tipo()).isEqualTo(FonteNormativaTipo.LEI);
        assertThat(fundamento.identificador()).isEqualTo("CLT art. 41");
    }
}
