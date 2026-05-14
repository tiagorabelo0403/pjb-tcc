package com.tcc.pjb.backend.service.responsavel;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ResponsavelAutoDesignationServiceTest {

    private final ResponsavelWorkloadBalancer balancer = new ResponsavelWorkloadBalancer();
    private final ResponsavelAutoDesignationService service =
            new ResponsavelAutoDesignationService(balancer);

    @Test
    void sugerir_comDesignacao_quandoServidoresDisponiveis() {
        var request = new ResponsavelAutoDesignationService.DesignacaoRequest(
                UUID.randomUUID(), "EXPEDIR",
                List.of(new ResponsavelWorkloadBalancer.ServidorCarga(
                        "srv-001", "Ana", 3, List.of())));
        var response = service.sugerir(request);
        assertThat(response.designacaoSugerida()).isTrue();
        assertThat(response.mensagem()).contains("confirmação");
    }

    @Test
    void sugerir_semDesignacao_quandoListaVazia() {
        var request = new ResponsavelAutoDesignationService.DesignacaoRequest(
                UUID.randomUUID(), "CERTIFICAR", List.of());
        var response = service.sugerir(request);
        assertThat(response.designacaoSugerida()).isFalse();
        assertThat(response.mensagem()).contains("manualmente");
    }
}
