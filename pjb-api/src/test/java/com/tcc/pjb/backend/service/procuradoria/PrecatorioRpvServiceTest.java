package com.tcc.pjb.backend.service.procuradoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.procuradoria.surface.PrecatorioRpvEnteDevedorTipo;
import com.tcc.pjb.backend.model.dto.procuradoria.surface.PrecatorioRpvNaturezaCredito;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.procuradoria.calendar.PrecatorioRpvCalendarPlanner;
import com.tcc.pjb.backend.service.procuradoria.queue.PrecatorioRpvQueuePlanner;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PrecatorioRpvServiceTest {

    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final PrecatorioRpvService service = new PrecatorioRpvService(processoRepository, new PrecatorioRpvQueuePlanner(), new PrecatorioRpvCalendarPlanner());

    @Test
    void deveClassificarPrecatórioSuperpreferencialEmFilaEspecial() {
        Processo processo = Processo.builder()
                .id(41L)
                .numeroProcesso("0000041-12.2026.8.06.0001")
                .tribunal("TJCE")
                .uf("CE")
                .valorCausa(new BigDecimal("150000.00"))
                .build();
        when(processoRepository.findById(41L)).thenReturn(Optional.of(processo));

        PrecatorioRpvService.PrecatorioRpvResponse response = service.calcular(new PrecatorioRpvService.PrecatorioRpvRequest(
                41L,
                new BigDecimal("150000.00"),
                new BigDecimal("0.03"),
                new BigDecimal("0.01"),
                new BigDecimal("0.05"),
                new BigDecimal("50000.00"),
                PrecatorioRpvNaturezaCredito.ALIMENTAR,
                PrecatorioRpvEnteDevedorTipo.ESTADO,
                "ESTADO_CE",
                LocalDate.of(2026, 4, 4),
                LocalDate.of(2026, 4, 4),
                LocalDate.of(1950, 1, 10),
                false,
                false,
                true,
                true
        ));

        assertThat(response.modalidade()).isEqualTo("PRECATORIO");
        assertThat(response.superpreferencia()).isTrue();
        assertThat(response.prioridadePagamento()).isEqualTo("SUPERPREFERENCIAL_ALIMENTAR");
        assertThat(response.planoFila().filaPrincipal()).isEqualTo("PRECATORIO_SUPERPREFERENCIAL_ALIMENTAR");
        assertThat(response.planoFila().filasOperacionais())
                .contains("PRECATORIO_CRONOLOGIA_REGIME_ESPECIAL_50", "PRECATORIO_ACORDO_DIRETO_ESTADO_CE");
        assertThat(response.planoFila().estagios()).extracting(stage -> stage.stageCode())
                .containsExactly("REQUISITORIO", "ORDENACAO", "ORCAMENTO", "LIBERACAO");
        assertThat(response.agendaOperacional().lanes()).extracting(lane -> lane.laneCode())
                .contains("PRAZO_FINANCEIRO", "FILA_OPERACIONAL", "GOVERNANCA");
    }

    @Test
    void deveAplicarSelicExclusivaAoCreditoTributario() {
        Processo processo = Processo.builder()
                .id(42L)
                .numeroProcesso("0000042-12.2026.8.06.0001")
                .tribunal("TRF5")
                .uf("PE")
                .valorCausa(new BigDecimal("100000.00"))
                .build();
        when(processoRepository.findById(42L)).thenReturn(Optional.of(processo));

        PrecatorioRpvService.PrecatorioRpvResponse response = service.calcular(new PrecatorioRpvService.PrecatorioRpvRequest(
                42L,
                new BigDecimal("100000.00"),
                new BigDecimal("0.03"),
                new BigDecimal("0.02"),
                new BigDecimal("0.04"),
                new BigDecimal("20000.00"),
                PrecatorioRpvNaturezaCredito.TRIBUTARIO,
                PrecatorioRpvEnteDevedorTipo.UNIAO,
                "UNIAO",
                LocalDate.of(2026, 4, 4),
                LocalDate.of(2026, 4, 4),
                null,
                false,
                false,
                false,
                false
        ));

        assertThat(response.politicaMonetaria().regraCalculo()).isEqualTo("SELIC_EXCLUSIVA");
        assertThat(response.correcao()).isEqualByComparingTo("4000.00");
        assertThat(response.juros()).isEqualByComparingTo("0.00");
        assertThat(response.totalAtualizado()).isEqualByComparingTo("104000.00");
        assertThat(response.planoFila().filaPrincipal()).isEqualTo("PRECATORIO_TRIBUTARIO_SELIC");
        assertThat(response.agendaOperacional().lanes()).extracting(lane -> lane.laneCode())
                .contains("PRAZO_FINANCEIRO", "FILA_OPERACIONAL", "GOVERNANCA");
    }

    @Test
    void deveRoterizarRpvComFilaDiretaEControleDePrazo() {
        Processo processo = Processo.builder()
                .id(43L)
                .numeroProcesso("0000043-12.2026.8.06.0001")
                .tribunal("TJCE")
                .uf("CE")
                .valorCausa(new BigDecimal("8000.00"))
                .build();
        when(processoRepository.findById(43L)).thenReturn(Optional.of(processo));

        PrecatorioRpvService.PrecatorioRpvResponse response = service.calcular(new PrecatorioRpvService.PrecatorioRpvRequest(
                43L,
                new BigDecimal("8000.00"),
                new BigDecimal("0.01"),
                new BigDecimal("0.00"),
                new BigDecimal("0.02"),
                new BigDecimal("15000.00"),
                PrecatorioRpvNaturezaCredito.COMUM,
                PrecatorioRpvEnteDevedorTipo.MUNICIPIO,
                "MUNICIPIO_MORADA_NOVA",
                LocalDate.of(2026, 4, 4),
                LocalDate.of(2026, 4, 4),
                null,
                false,
                false,
                false,
                false
        ));

        assertThat(response.modalidade()).isEqualTo("RPV");
        assertThat(response.prazoMaximoPagamento()).isEqualTo(LocalDate.of(2026, 6, 4));
        assertThat(response.planoFila().filaPrincipal()).isEqualTo("RPV_EXECUCAO_DIRETA");
        assertThat(response.planoFila().filasOperacionais())
                .contains("RPV_CONTROLE_PRAZO_2M", "RPV_ALVARA_E_LIBERACAO");
        assertThat(response.planoFila().estagios()).extracting(stage -> stage.stageCode())
                .containsExactly("REQUISITORIO", "PROCESSAMENTO", "LIBERACAO");
        assertThat(response.agendaOperacional().lanes()).extracting(lane -> lane.laneCode())
                .contains("PRAZO_FINANCEIRO", "FILA_OPERACIONAL", "GOVERNANCA");
    }
}
