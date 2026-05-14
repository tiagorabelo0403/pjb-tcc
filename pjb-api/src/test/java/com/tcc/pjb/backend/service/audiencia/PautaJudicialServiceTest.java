package com.tcc.pjb.backend.service.audiencia;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PautaJudicialServiceTest {

    private final PautaJudicialService service = new PautaJudicialService();

    @Test
    void consultarPauta_retornaSlotsNoIntervalo() {
        var slots = List.of(
                slot(AudienciaTipo.CONCILIACAO, LocalDateTime.of(2025, 6, 10, 9, 0), true),
                slot(AudienciaTipo.CONCILIACAO, LocalDateTime.of(2025, 6, 15, 14, 0), true),
                slot(AudienciaTipo.INSTRUCAO_E_JULGAMENTO, LocalDateTime.of(2025, 7, 1, 9, 0), true));
        var consulta = new PautaJudicialService.PautaConsulta(
                "VARA-001",
                LocalDateTime.of(2025, 6, 1, 0, 0).toLocalDate(),
                LocalDateTime.of(2025, 6, 30, 0, 0).toLocalDate(),
                null);
        var result = service.consultarPauta(consulta, slots);
        assertThat(result.slots()).hasSize(2);
        assertThat(result.totalDisponivel()).isEqualTo(2);
    }

    @Test
    void consultarPauta_filtraPorTipo() {
        var slots = List.of(
                slot(AudienciaTipo.CONCILIACAO, LocalDateTime.of(2025, 6, 10, 9, 0), true),
                slot(AudienciaTipo.INSTRUCAO_E_JULGAMENTO, LocalDateTime.of(2025, 6, 15, 9, 0), true));
        var consulta = new PautaJudicialService.PautaConsulta(
                "VARA-001",
                LocalDateTime.of(2025, 6, 1, 0, 0).toLocalDate(),
                LocalDateTime.of(2025, 6, 30, 0, 0).toLocalDate(),
                AudienciaTipo.CONCILIACAO);
        var result = service.consultarPauta(consulta, slots);
        assertThat(result.slots()).hasSize(1);
        assertThat(result.slots().getFirst().tipo()).isEqualTo(AudienciaTipo.CONCILIACAO);
    }

    @Test
    void designar_sucesso_quandoSlotDisponivel() {
        var slotDisponivel = slot(AudienciaTipo.CONCILIACAO,
                LocalDateTime.of(2025, 8, 10, 9, 0), true);
        var req = new PautaJudicialService.DesignacaoRequest(
                UUID.randomUUID(), slotDisponivel.slotId(),
                AudienciaTipo.CONCILIACAO, List.of("adv-001"), true);
        var result = service.designar(req, slotDisponivel);
        assertThat(result.sucesso()).isTrue();
        assertThat(result.audienciaId()).isNotNull();
    }

    @Test
    void designar_falha_quandoSlotIndisponivel() {
        var slotOcupado = slot(AudienciaTipo.CONCILIACAO,
                LocalDateTime.of(2025, 8, 10, 9, 0), false);
        var req = new PautaJudicialService.DesignacaoRequest(
                UUID.randomUUID(), slotOcupado.slotId(),
                AudienciaTipo.CONCILIACAO, List.of(), false);
        var result = service.designar(req, slotOcupado);
        assertThat(result.sucesso()).isFalse();
        assertThat(result.pendencia()).contains("indisponível");
    }

    @Test
    void designar_falha_virtualParaTipoPresencial() {
        var slotPresencial = slot(AudienciaTipo.CUSTODIAL,
                LocalDateTime.of(2025, 8, 10, 9, 0), true);
        var req = new PautaJudicialService.DesignacaoRequest(
                UUID.randomUUID(), slotPresencial.slotId(),
                AudienciaTipo.CUSTODIAL, List.of(), true);
        var result = service.designar(req, slotPresencial);
        assertThat(result.sucesso()).isFalse();
        assertThat(result.pendencia()).contains("não admite modalidade virtual");
    }

    private PautaJudicialService.SlotPauta slot(AudienciaTipo tipo, LocalDateTime inicio, boolean disponivel) {
        return new PautaJudicialService.SlotPauta(
                UUID.randomUUID(), inicio,
                inicio.plusMinutes(tipo.duracaoEstimadaMinutos()),
                tipo, disponivel, tipo.admiteVirtual(), "Sala 1");
    }
}
