package com.tcc.pjb.backend.service.processual.acceleration.eleitoral;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.processual.acceleration.eleitoral.EleitoralPrazoCriticoService.AlertaEleitoral;
import com.tcc.pjb.backend.service.processual.acceleration.eleitoral.EleitoralPrazoCriticoService.ProcessoEleitoralItem;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EleitoralPrazoCriticoServiceTest {

    private final EleitoralPrazoCriticoService service = new EleitoralPrazoCriticoService();

    @Test
    void deveClassificarComoCriticoComAte3Dias() {
        List<ProcessoEleitoralItem> processos = List.of(
                new ProcessoEleitoralItem(UUID.randomUUID(), "0001",
                        LocalDate.now().plusDays(30), LocalDate.now().plusDays(2), false));

        List<AlertaEleitoral> alertas = service.identificarCriticos(processos);

        assertThat(alertas).hasSize(1);
        assertThat(alertas.get(0).nivel()).isEqualTo("CRITICO");
        assertThat(alertas.get(0).diasRestantes()).isEqualTo(2);
    }

    @Test
    void deveClassificarComoUrgenteEntre4E10Dias() {
        List<ProcessoEleitoralItem> processos = List.of(
                new ProcessoEleitoralItem(UUID.randomUUID(), "0002",
                        LocalDate.now().plusDays(60), LocalDate.now().plusDays(7), false));

        List<AlertaEleitoral> alertas = service.identificarCriticos(processos);

        assertThat(alertas.get(0).nivel()).isEqualTo("URGENTE");
    }

    @Test
    void deveClassificarComoAtencaoAcimaDe10Dias() {
        List<ProcessoEleitoralItem> processos = List.of(
                new ProcessoEleitoralItem(UUID.randomUUID(), "0003",
                        LocalDate.now().plusDays(90), LocalDate.now().plusDays(20), false));

        List<AlertaEleitoral> alertas = service.identificarCriticos(processos);

        assertThat(alertas.get(0).nivel()).isEqualTo("ATENCAO");
    }

    @Test
    void deveExcluirProcessosComPrazoVencido() {
        List<ProcessoEleitoralItem> processos = List.of(
                new ProcessoEleitoralItem(UUID.randomUUID(), "0004",
                        LocalDate.now().minusDays(10), LocalDate.now().minusDays(1), false));

        List<AlertaEleitoral> alertas = service.identificarCriticos(processos);

        assertThat(alertas).isEmpty();
    }

    @Test
    void deveOrdenarPorDiasRestantesAscendente() {
        List<ProcessoEleitoralItem> processos = List.of(
                new ProcessoEleitoralItem(UUID.randomUUID(), "urgente",
                        LocalDate.now().plusDays(30), LocalDate.now().plusDays(5), false),
                new ProcessoEleitoralItem(UUID.randomUUID(), "critico",
                        LocalDate.now().plusDays(30), LocalDate.now().plusDays(1), false));

        List<AlertaEleitoral> alertas = service.identificarCriticos(processos);

        assertThat(alertas.get(0).diasRestantes()).isLessThan(alertas.get(1).diasRestantes());
    }

    @Test
    void deveExcluirProcessosSemPrazoDecisao() {
        List<ProcessoEleitoralItem> processos = List.of(
                new ProcessoEleitoralItem(UUID.randomUUID(), "0005",
                        LocalDate.now().plusDays(30), null, false));

        List<AlertaEleitoral> alertas = service.identificarCriticos(processos);

        assertThat(alertas).isEmpty();
    }
}
