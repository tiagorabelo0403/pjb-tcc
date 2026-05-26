package com.tcc.pjb.backend.integration.serpro.datavalid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CpfValidacaoServiceTest {

    private CpfValidacaoPort port;
    private CpfValidacaoService service;

    @BeforeEach
    void setUp() {
        port = mock(CpfValidacaoPort.class);
        service = new CpfValidacaoService(port);
    }

    @Test
    void cpfRegular_naoBloqueia() {
        when(port.consultar("12345678901")).thenReturn(new CpfValidacaoResult(CpfSituacao.REGULAR));
        assertThatNoException().isThrownBy(() -> service.validarParaPeticionamento("12345678901"));
    }

    @Test
    void cpfTitularFalecido_lancaExcecaoComCodigoPjbCpf001() {
        when(port.consultar("12345678901")).thenReturn(new CpfValidacaoResult(CpfSituacao.CANCELADA_POR_OBITO));
        assertThatThrownBy(() -> service.validarParaPeticionamento("12345678901"))
                .isInstanceOfSatisfying(CpfSituacaoBloqueadaException.class,
                        ex -> assertThat(ex.getCodigoPjb()).isEqualTo("PJB-CPF-001"));
    }

    @Test
    void cpfSuspenso_lancaExcecaoComCodigoPjbCpf002() {
        when(port.consultar("12345678901")).thenReturn(new CpfValidacaoResult(CpfSituacao.SUSPENSA));
        assertThatThrownBy(() -> service.validarParaPeticionamento("12345678901"))
                .isInstanceOfSatisfying(CpfSituacaoBloqueadaException.class,
                        ex -> assertThat(ex.getCodigoPjb()).isEqualTo("PJB-CPF-002"));
    }

    @Test
    void cpfNulo_lancaExcecaoComCodigoPjbCpf002() {
        when(port.consultar("00000000000")).thenReturn(new CpfValidacaoResult(CpfSituacao.NULA));
        assertThatThrownBy(() -> service.validarParaPeticionamento("00000000000"))
                .isInstanceOfSatisfying(CpfSituacaoBloqueadaException.class,
                        ex -> assertThat(ex.getCodigoPjb()).isEqualTo("PJB-CPF-002"));
    }

    @Test
    void mensagemExcecao_naoContemPalavraObito() {
        when(port.consultar("12345678901")).thenReturn(new CpfValidacaoResult(CpfSituacao.CANCELADA_POR_OBITO));
        assertThatThrownBy(() -> service.validarParaPeticionamento("12345678901"))
                .isInstanceOf(CpfSituacaoBloqueadaException.class)
                .hasMessageNotContainingAny("óbito", "obito", "Óbito", "OBITO",
                        "falecido", "falecimento", "morte", "Morte");
    }

    @Test
    void portFalha_servicoBloqueiaComCpf002() {
        when(port.consultar(anyString())).thenThrow(new RuntimeException("timeout"));
        assertThatThrownBy(() -> service.validarParaPeticionamento("12345678901"))
                .isInstanceOfSatisfying(CpfSituacaoBloqueadaException.class,
                        ex -> assertThat(ex.getCodigoPjb()).isEqualTo("PJB-CPF-002"));
    }
}
