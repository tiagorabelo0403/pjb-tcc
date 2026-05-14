package com.tcc.pjb.backend.service.processual.note;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProcessoPendingNotePanelServiceTest {

    private final ProcessoPendingNotePanelService service = new ProcessoPendingNotePanelService();

    @Test
    void construir_identificaAtrasadas_quandoDueAtNoPassado() {
        var nota = new ProcessoPendingNotePanelService.NotaPendente(
                1L, 100L, ProcessoNoteType.PENDENCIA, "regularizar",
                Instant.now().minusSeconds(86400), 1);
        var painel = service.construir(List.of(nota));
        assertThat(painel.atrasadas()).hasSize(1);
        assertThat(painel.vencendo()).isEmpty();
    }

    @Test
    void construir_identificaVencendo_quandoDueAtEm2Dias() {
        var nota = new ProcessoPendingNotePanelService.NotaPendente(
                2L, 100L, ProcessoNoteType.LEMBRETE, "reuniao",
                Instant.now().plusSeconds(86400 * 2), 2);
        var painel = service.construir(List.of(nota));
        assertThat(painel.vencendo()).hasSize(1);
        assertThat(painel.atrasadas()).isEmpty();
    }

    @Test
    void construir_contagemPorTipo_correta() {
        var notas = List.of(
                new ProcessoPendingNotePanelService.NotaPendente(
                        3L, 100L, ProcessoNoteType.PENDENCIA, "p1", null, 1),
                new ProcessoPendingNotePanelService.NotaPendente(
                        4L, 100L, ProcessoNoteType.PENDENCIA, "p2", null, 1),
                new ProcessoPendingNotePanelService.NotaPendente(
                        5L, 100L, ProcessoNoteType.ALERTA_OPERACIONAL, "alerta", null, 3));
        var painel = service.construir(notas);
        assertThat(painel.contagemPorTipo()).containsEntry(ProcessoNoteType.PENDENCIA, 2L);
        assertThat(painel.total()).isEqualTo(3);
    }
}
