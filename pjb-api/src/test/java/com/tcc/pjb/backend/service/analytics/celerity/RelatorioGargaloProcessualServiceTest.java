package com.tcc.pjb.backend.service.analytics.celerity;

import com.tcc.pjb.backend.service.processual.celerity.ProcessoGargaloTipo;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RelatorioGargaloProcessualServiceTest {

    private final RelatorioGargaloProcessualService service = new RelatorioGargaloProcessualService();

    @Test
    void gerar_relatorioComGargalos_quandoHouverContagens() {
        var orgaoId = UUID.randomUUID();
        var contagens = Map.of(
                ProcessoGargaloTipo.EXPEDIENTE_SEM_CIENCIA, 50L,
                ProcessoGargaloTipo.MANDADO_DEVOLVIDO_SEM_CUMPRIMENTO, 20L);
        var relatorio = service.gerar(orgaoId, "2025-05", contagens);
        assertThat(relatorio.gargalos()).hasSize(2);
        assertThat(relatorio.gargalos().getFirst().quantidade()).isEqualTo(50L);
        assertThat(relatorio.observacao()).contains("sem identificação individual");
    }

    @Test
    void gerar_percentualCorreto_paraGargaloDominante() {
        var contagens = Map.of(
                ProcessoGargaloTipo.TAREFA_SEM_RESPONSAVEL, 100L,
                ProcessoGargaloTipo.CUSTAS_PENDENTES, 0L);
        var relatorio = service.gerar(UUID.randomUUID(), "2025-05", contagens);
        var tarefa = relatorio.gargalos().stream()
                .filter(g -> g.tipo() == ProcessoGargaloTipo.TAREFA_SEM_RESPONSAVEL)
                .findFirst().orElseThrow();
        assertThat(tarefa.percentualDoTotal()).isEqualTo(100.0);
    }

    @Test
    void gerar_semGargalos_quandoContagensVazias() {
        var relatorio = service.gerar(UUID.randomUUID(), "2025-05", Map.of());
        assertThat(relatorio.gargalos()).isEmpty();
    }
}
