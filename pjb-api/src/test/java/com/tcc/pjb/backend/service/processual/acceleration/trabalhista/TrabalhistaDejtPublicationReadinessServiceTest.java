package com.tcc.pjb.backend.service.processual.acceleration.trabalhista;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.processual.acceleration.trabalhista.TrabalhistaDejtPublicationReadinessService.DejtInput;
import com.tcc.pjb.backend.service.processual.acceleration.trabalhista.TrabalhistaDejtPublicationReadinessService.DejtReadiness;
import org.junit.jupiter.api.Test;

class TrabalhistaDejtPublicationReadinessServiceTest {

    private final TrabalhistaDejtPublicationReadinessService service =
            new TrabalhistaDejtPublicationReadinessService();

    @Test
    void deveRetornarAptoQuandoTodosRequisitosAtendidos() {
        DejtInput input = new DejtInput(true, true, true, true, true);

        DejtReadiness result = service.avaliar(input);

        assertThat(result.apto()).isTrue();
        assertThat(result.pendencias()).isEmpty();
        assertThat(result.mensagem()).contains("apta");
    }

    @Test
    void deveAdicionarPendenciaQuandoDecisaoNaoAssinada() {
        DejtInput input = new DejtInput(false, true, true, true, true);

        DejtReadiness result = service.avaliar(input);

        assertThat(result.apto()).isFalse();
        assertThat(result.pendencias()).anyMatch(p -> p.contains("assinada"));
    }

    @Test
    void deveAdicionarPendenciaQuandoPartesNaoCadastradasDeJt() {
        DejtInput input = new DejtInput(true, false, true, true, true);

        DejtReadiness result = service.avaliar(input);

        assertThat(result.pendencias()).anyMatch(p -> p.contains("Partes sem cadastro"));
    }

    @Test
    void deveAdicionarPendenciaParaSigiloIncompativel() {
        DejtInput input = new DejtInput(true, true, true, false, true);

        DejtReadiness result = service.avaliar(input);

        assertThat(result.pendencias()).anyMatch(p -> p.contains("sigilo"));
    }

    @Test
    void deveMensagemContarPendencias() {
        DejtInput input = new DejtInput(false, false, false, false, false);

        DejtReadiness result = service.avaliar(input);

        assertThat(result.apto()).isFalse();
        assertThat(result.pendencias()).hasSize(5);
        assertThat(result.mensagem()).contains("5");
    }
}
