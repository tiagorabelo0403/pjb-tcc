package com.tcc.pjb.backend.service.processual.sobrestamento;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SobrestamentoInteligenteServiceTest {

    private final SobrestamentoInteligenteService service = new SobrestamentoInteligenteService();

    @Test
    void avaliar_motivoCessou_quandoFlagAtivo() {
        var input = new SobrestamentoInteligenteService.SobrestamentoInput(
                UUID.randomUUID(), "0001-12.2025.1.00.0001",
                "julgamento de repetitivo", "PROC-REP-001", true, "1234");
        var status = service.avaliar(input);
        assertThat(status.motivoCessou()).isTrue();
        assertThat(status.acaoSugerida()).contains("reativar");
    }

    @Test
    void avaliar_motivoNaoCessou_quandoFlagFalso() {
        var input = new SobrestamentoInteligenteService.SobrestamentoInput(
                UUID.randomUUID(), "0001-12.2025.1.00.0002",
                "acórdão pendente", "PROC-REP-002", false, "5678");
        var status = service.avaliar(input);
        assertThat(status.motivoCessou()).isFalse();
        assertThat(status.acaoSugerida()).contains("acórdão pendente");
    }

    @Test
    void varrer_processaTodosProcessos() {
        var processos = List.of(
                new SobrestamentoInteligenteService.SobrestamentoInput(
                        UUID.randomUUID(), "0001", "motivo-A", null, true, null),
                new SobrestamentoInteligenteService.SobrestamentoInput(
                        UUID.randomUUID(), "0002", "motivo-B", null, false, null));
        var resultados = service.varrer(processos);
        assertThat(resultados).hasSize(2);
        assertThat(resultados.stream().filter(SobrestamentoInteligenteService.SobrestamentoStatus::motivoCessou).count())
                .isEqualTo(1);
    }
}
