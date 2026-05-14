package com.tcc.pjb.backend.service.mandados;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CentralMandadosAccelerationServiceTest {

    private final MandadoReadinessService readinessService = new MandadoReadinessService();
    private final MandadoReturnDiagnosisService diagnosisService = new MandadoReturnDiagnosisService();
    private final CentralMandadosAccelerationService service =
            new CentralMandadosAccelerationService(readinessService, diagnosisService);

    @Test
    void identificarPrioritarios_filtraMandadosNormais() {
        var mandados = List.of(
                mandado(false, false, false, false),
                mandado(true, false, false, false));
        var prioritarios = service.identificarPrioritarios(mandados);
        assertThat(prioritarios).hasSize(1);
    }

    @Test
    void identificarPrioritarios_maisPrioridade_quandoReuPresoEPrazoVencido() {
        var mandados = List.of(
                mandado(true, false, true, false),
                mandado(true, false, true, true));
        var prioritarios = service.identificarPrioritarios(mandados);
        assertThat(prioritarios.getFirst().prioridade())
                .isGreaterThan(prioritarios.getLast().prioridade());
    }

    @Test
    void identificarPrioritarios_acaoDevolvido_quandoDevolvidoFlag() {
        var mandados = List.of(mandado(true, false, false, false));
        var prioritarios = service.identificarPrioritarios(mandados);
        assertThat(prioritarios.getFirst().acao()).contains("Diagnosticar devolução");
    }

    @Test
    void identificarPrioritarios_semPrioritarios_quandoTudoOk() {
        var mandados = List.of(mandado(false, false, false, false));
        var prioritarios = service.identificarPrioritarios(mandados);
        assertThat(prioritarios).isEmpty();
    }

    private CentralMandadosAccelerationService.MandadoItem mandado(
            boolean devolvido, boolean semCumprimento, boolean prazoVencido, boolean reuPreso) {
        return new CentralMandadosAccelerationService.MandadoItem(
                UUID.randomUUID(), "0001234-12.2025.8.06.0001",
                devolvido, semCumprimento, prazoVencido, reuPreso);
    }
}
