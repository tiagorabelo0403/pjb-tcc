package com.tcc.pjb.backend.service.processual.acceleration.execucao;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.processual.acceleration.execucao.ExecucaoFiscalPrescricaoIntercorrenteRadarService.AlertaPrescricao;
import com.tcc.pjb.backend.service.processual.acceleration.execucao.ExecucaoFiscalPrescricaoIntercorrenteRadarService.ExecucaoFiscalItem;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExecucaoFiscalPrescricaoIntercorrenteRadarServiceTest {

    private final ExecucaoFiscalPrescricaoIntercorrenteRadarService service =
            new ExecucaoFiscalPrescricaoIntercorrenteRadarService();

    @Test
    void deveRetornarListaVaziaParaProcessosDentroDoLimiar() {
        List<ExecucaoFiscalItem> processos = List.of(
                new ExecucaoFiscalItem(UUID.randomUUID(), "0001", Duration.ofDays(100), false),
                new ExecucaoFiscalItem(UUID.randomUUID(), "0002", Duration.ofDays(200), false));

        List<AlertaPrescricao> alertas = service.radar(processos);

        assertThat(alertas).isEmpty();
    }

    @Test
    void deveGerarAlertaAtencaoParaProcessoParadoEntre365E1460Dias() {
        UUID id = UUID.randomUUID();
        List<ExecucaoFiscalItem> processos = List.of(
                new ExecucaoFiscalItem(id, "1234", Duration.ofDays(500), false));

        List<AlertaPrescricao> alertas = service.radar(processos);

        assertThat(alertas).hasSize(1);
        assertThat(alertas.get(0).nivel()).isEqualTo("ATENCAO");
        assertThat(alertas.get(0).exigeAcaoImediata()).isFalse();
        assertThat(alertas.get(0).processoId()).isEqualTo(id);
    }

    @Test
    void deveGerarAlertaCriticoParaProcessoParadoAcimaDe1460Dias() {
        UUID id = UUID.randomUUID();
        List<ExecucaoFiscalItem> processos = List.of(
                new ExecucaoFiscalItem(id, "5678", Duration.ofDays(1500), false));

        List<AlertaPrescricao> alertas = service.radar(processos);

        assertThat(alertas).hasSize(1);
        assertThat(alertas.get(0).nivel()).isEqualTo("CRITICO");
        assertThat(alertas.get(0).exigeAcaoImediata()).isTrue();
    }

    @Test
    void devePriorizarCriticosAntesDeAtencao() {
        List<ExecucaoFiscalItem> processos = List.of(
                new ExecucaoFiscalItem(UUID.randomUUID(), "A", Duration.ofDays(500), false),
                new ExecucaoFiscalItem(UUID.randomUUID(), "B", Duration.ofDays(1500), false));

        List<AlertaPrescricao> alertas = service.radar(processos);

        assertThat(alertas.get(0).nivel()).isEqualTo("CRITICO");
        assertThat(alertas.get(1).nivel()).isEqualTo("ATENCAO");
    }

    @Test
    void deveFiltrarProcessosAbaixoDoLimiarDe365Dias() {
        List<ExecucaoFiscalItem> processos = List.of(
                new ExecucaoFiscalItem(UUID.randomUUID(), "X", Duration.ofDays(364), false),
                new ExecucaoFiscalItem(UUID.randomUUID(), "Y", Duration.ofDays(365), false));

        List<AlertaPrescricao> alertas = service.radar(processos);

        assertThat(alertas).hasSize(1);
        assertThat(alertas.get(0).numeroProcesso()).isEqualTo("Y");
    }
}
