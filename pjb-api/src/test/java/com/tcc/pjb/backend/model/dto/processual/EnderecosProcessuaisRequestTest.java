package com.tcc.pjb.backend.model.dto.processual;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.enums.processual.CriterioTerritorial;
import org.junit.jupiter.api.Test;

class EnderecosProcessuaisRequestTest {

    @Test
    void ancoraParaLocalPrestacaoServicoDevolveALocalPrestacaoServico() {
        AncoraTerritorial localPrestacaoServico = new AncoraTerritorial(null, "Morada Nova", "CE");
        EnderecosProcessuaisRequest enderecos = new EnderecosProcessuaisRequest(null, null, localPrestacaoServico, null, false);

        assertThat(enderecos.ancoraPara(CriterioTerritorial.LOCAL_PRESTACAO_SERVICO)).isEqualTo(localPrestacaoServico);
    }

    @Test
    void ancoraParaDomicilioReuComFlagDesconhecidoDevolveNuloMesmoPreenchido() {
        AncoraTerritorial domicilioReu = new AncoraTerritorial(null, "Sao Paulo", "SP");
        EnderecosProcessuaisRequest enderecos = new EnderecosProcessuaisRequest(null, domicilioReu, null, null, true);

        assertThat(enderecos.ancoraPara(CriterioTerritorial.DOMICILIO_REU)).isNull();
    }

    @Test
    void ancoraParaSituacaoDaCoisaDevolveLocalDoFato() {
        AncoraTerritorial localDoFato = new AncoraTerritorial(null, "Fortaleza", "CE");
        EnderecosProcessuaisRequest enderecos = new EnderecosProcessuaisRequest(null, null, null, localDoFato, false);

        assertThat(enderecos.ancoraPara(CriterioTerritorial.SITUACAO_DA_COISA)).isEqualTo(localDoFato);
    }

    @Test
    void vazioDevolveInstanciaComTodasAsAncorasNulasEFlagFalsa() {
        EnderecosProcessuaisRequest enderecos = EnderecosProcessuaisRequest.vazio();

        assertThat(enderecos.domicilioAutor()).isNull();
        assertThat(enderecos.domicilioReu()).isNull();
        assertThat(enderecos.localPrestacaoServico()).isNull();
        assertThat(enderecos.localDoFato()).isNull();
        assertThat(enderecos.domicilioReuDesconhecido()).isFalse();
    }
}
