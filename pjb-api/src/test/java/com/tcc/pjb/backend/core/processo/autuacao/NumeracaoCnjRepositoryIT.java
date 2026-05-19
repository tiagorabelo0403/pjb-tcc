package com.tcc.pjb.backend.core.processo.autuacao;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.core.processo.autuacao.application.NumeracaoCnjApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NumeracaoCnjRepositoryIT extends PjbIntegrationTestBase {

    @Autowired private NumeracaoCnjApplicationService service;

    @Test
    void it01_gerarProximoRetornaNumeroNoFormatoCNJ() {
        String numero = service.gerarProximo(8, 6, 1);
        assertThat(numero).matches("\\d{7}-\\d{2}\\.\\d{4}\\.8\\.06\\.0001");
    }

    @Test
    void it02_segundaGeracaoMesmoTribunalValida() {
        service.gerarProximo(8, 7, 99);
        String segunda = service.gerarProximo(8, 7, 99);
        assertThat(service.validar(segunda)).isTrue();
    }

    @Test
    void it03_validarNumeroCNJGeradoRetornaTrue() {
        String numero = service.gerarProximo(8, 26, 200);
        assertThat(service.validar(numero)).isTrue();
    }

    @Test
    void it04_validarNumeroComDigitoErradoRetornaFalse() {
        assertThat(service.validar("0000000-00.2024.8.06.0001")).isFalse();
    }

    @Test
    void it05_geracoesDiferentesTribunaisIndependentes() {
        String n1 = service.gerarProximo(8, 6, 500);
        String n2 = service.gerarProximo(8, 26, 500);
        assertThat(service.validar(n1)).isTrue();
        assertThat(service.validar(n2)).isTrue();
        assertThat(n1).isNotEqualTo(n2);
    }
}
